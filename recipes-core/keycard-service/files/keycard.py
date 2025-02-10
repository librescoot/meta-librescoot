import os
import pty
import shlex
import subprocess
import threading
import time
from PN7150 import PN7150

# Assuming the output from the NFC reader contains lines that look like "NFCID1 : [UID_VALUE]"
_OUTPUT_UID_PREFIX = 'NFCID1 :'
AUTHORIZED_UIDS_FILE = '/data/keycard/authorized_uids.txt'
MASTER_UIDS_FILE = '/data/keycard/master_uids.txt'
KEYCARD_SCRIPT = '/usr/bin/keycard.sh'
LED_SCRIPT = '/usr/bin/ledcontrol.sh'
GREEN_LED_SCRIPT = '/usr/bin/greenled.sh'
_CMD_POLL = '/usr/sbin/nfcDemoApp poll'


class PN7150Extended(PN7150):
    def __init__(self, nfc_demo_app_location='/usr/sbin'):
        super().__init__(nfc_demo_app_location)
        self._green_led_thread = None
        self._green_led_running = False
        self._learn_mode = False
        self._newUIDs = []
        
        self._master_learning_mode = not os.path.exists(MASTER_UIDS_FILE)
        if not self._master_learning_mode:
            self.master_uids = self._load_master_uids(MASTER_UIDS_FILE)
            subprocess.run([GREEN_LED_SCRIPT, '0'], shell=False)
        else:
            self.master_uids = []
            print("No master UIDs file found. Entering master learning mode...")
            # Start blinking green LED
            self._start_green_led_blink()
            
        self.authorized_uids = self._load_authorized_uids(AUTHORIZED_UIDS_FILE)

    def _start_green_led_blink(self):
        self._green_led_running = True
        self._green_led_thread = threading.Thread(target=self._green_led_blink)
        self._green_led_thread.daemon = True
        self._green_led_thread.start()

    def _stop_green_led_blink(self):
        self._green_led_running = False
        if self._green_led_thread and self._green_led_thread.is_alive():
            self._green_led_thread.join()
        subprocess.run([GREEN_LED_SCRIPT, '0'], shell=False)

    def _green_led_blink(self):
        while self._green_led_running:
            try:
                print("Blink")
                subprocess.run([GREEN_LED_SCRIPT, '1'], shell=False, check=True)
                time.sleep(0.5)
                subprocess.run([GREEN_LED_SCRIPT, '0'], shell=False, check=True)
                time.sleep(0.5)
            except subprocess.CalledProcessError:
                print("Error controlling LED")
                time.sleep(1)  # Avoid tight loop if LED control is failing

    @staticmethod
    def _load_authorized_uids(file_path):
        try:
            with open(file_path, 'r') as file:
                return [line.strip() for line in file.readlines()]
        except FileNotFoundError:
            return []

    @staticmethod
    def _load_master_uids(file_path):
        with open(file_path, 'r') as file:
            return [line.strip() for line in file.readlines()]

    @staticmethod
    def _write_authorized_uids(file_path, uids):
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        with open(file_path, 'w') as file:
            for uid in uids:
                file.write("%s\n" % uid)

    @staticmethod
    def _write_master_uids(file_path, uid):
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        with open(file_path, 'w') as file:
            file.write("%s\n" % uid)

    def _read_thread(self):
        cmd = _CMD_POLL.format(nfc_demo_app_path=self._nfc_demo_app_path)
        master, self._slave = pty.openpty()
        self._proc = subprocess.Popen(shlex.split(cmd), stdin=subprocess.PIPE, stdout=self._slave, stderr=self._slave)
        stdout = os.fdopen(master)

        self._read_running = True
        while self._read_running:
            try:
                line = stdout.readline()
                if _OUTPUT_UID_PREFIX in line:
                    uid = line.split(_OUTPUT_UID_PREFIX)[-1].strip().replace('\'', '').strip()
                    
                    if self._master_learning_mode:
                        print(f"Learning first master UID: {uid}")
                        self._write_master_uids(MASTER_UIDS_FILE, uid)
                        self.master_uids = [uid]
                        self._master_learning_mode = False
                        self._stop_green_led_blink()
                        subprocess.run([LED_SCRIPT, '3', '0'], shell=False) # blink
                        subprocess.run([LED_SCRIPT, '7', '0'], shell=False) # blink 
                        print("Master UID learned and saved. Switching to normal mode.")
                        continue

                    if not self._learn_mode:
                        if uid in self.master_uids:
                            print(f"Master UID detected: {uid} - Switching to learn mode")
                            self._learn_mode = True
                            subprocess.run([LED_SCRIPT, '3', '1'], shell=False) # blink
                            subprocess.run([LED_SCRIPT, '7', '1'], shell=False) # blink
                        elif uid in self.authorized_uids:
                            print(f"Authorized UID detected: {uid} - Executing script")
                            subprocess.run([GREEN_LED_SCRIPT, '1'], shell=False)
                            subprocess.run([KEYCARD_SCRIPT], shell=False)
                            time.sleep(1)
                            subprocess.run([GREEN_LED_SCRIPT, '0'], shell=False)
                        else:
                            print(f"Unauthorized UID detected: {uid}")
                    else:
                        if uid in self.master_uids:
                            print(f"Master UID detected: {uid} - Switching off learn mode")
                            self._learn_mode = False
                            subprocess.run([LED_SCRIPT, '3', '2'], shell=False) # off
                            subprocess.run([LED_SCRIPT, '7', '2'], shell=False) # off
                            if len(self._newUIDs) == 0:
                                print('nothing learned')
                            else:
                                for newUID in self._newUIDs:
                                    print(newUID)
                                self._write_authorized_uids(AUTHORIZED_UIDS_FILE, self._newUIDs)
                                self.authorized_uids = self._load_authorized_uids(AUTHORIZED_UIDS_FILE)
                            self._newUIDs = []
                        else:
                            print(f"UID detected: {uid} - Learning")
                            subprocess.run([GREEN_LED_SCRIPT, '1'], shell=False)
                            self._newUIDs.append(uid)
                            subprocess.run([LED_SCRIPT, '1', '0'], shell=False) # blink
                            subprocess.run([LED_SCRIPT, '1', '0'], shell=False) # blink
                            subprocess.run([LED_SCRIPT, '1', '0'], shell=False) # blink
                            time.sleep(1)
                            subprocess.run([GREEN_LED_SCRIPT, '0'], shell=False)

            except (IOError, OSError):
                pass

    def start_reading(self):
        if self._master_learning_mode:
            print("Starting NFC tag reading in master learning mode...")
        else:
            print("Starting NFC tag reading with UID checking...")
        super().start_reading()

# Example usage
if __name__ == "__main__":
    pn7150 = PN7150Extended()
    pn7150.start_reading()

