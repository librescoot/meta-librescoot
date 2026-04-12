#!/usr/bin/env python3

import argparse
from binascii import hexlify
from crccheck.crc import CrcArc as crc16
from serial import Serial
from struct import pack
import time

serial_baud_rate = 115200
serial_rx_timeout = 1
cbor_dfu = bytearray([0xA1, 0x19, 0xF0, 0x00, 0xF6])
slip_end = bytearray([0xC0])
reboot_delay = 1
# Duration of the wake-up break signal sent before the DFU command. A 500ms
# TX-low pulse recovers the nRF from a hung USOCK state machine — needed
# after a failed DFU transaction, a stuck app, or a suspended radio.
wake_break_duration = 0.5
wake_settle_delay = 1.0


def print_packet(packet):
    hexstr = hexlify(bytes(packet)).decode()
    print(' '.join(hexstr[i:i + 2] for i in range(0, len(hexstr), 2)))


def encode_usock(payload):
    header = bytearray()
    header.append(0xF6)
    header.append(0xD9)
    header.append(0x00)
    header.extend(pack('<H', len(payload)))
    header_crc = crc16.calcbytes(header, byteorder='little')
    payload_crc = crc16.calcbytes(payload, byteorder='little')
    return header + header_crc + payload + payload_crc


def serial_request(tx_packet):
    serial = Serial(port=args.port,
                    baudrate=serial_baud_rate,
                    timeout=serial_rx_timeout)
    serial.reset_input_buffer()
    print("  Tx:", end=' ')
    print_packet(tx_packet)
    serial.write(tx_packet)
    # Read a bounded number of bytes. The nRF's data stream may still be
    # active when we send the DFU command, so an unbounded read-until-silence
    # loop would hang for as long as telemetry frames keep arriving. 256
    # bytes is more than enough for any single USOCK response frame.
    rx_packet = serial.read(256)
    serial.close()
    print("  Rx:", end=' ')
    print_packet(rx_packet)
    return rx_packet


def wake_nrf():
    """Send a UART break to recover a hung nRF USOCK state machine.

    If the nRF is already responsive this is a harmless framing error that its
    USOCK driver ignores. If the nRF is wedged, the break is the only reliable
    way to kick it back into a cooperative state before we send the DFU command.
    """
    print("Sending wake-up break...")
    serial = Serial(port=args.port,
                    baudrate=serial_baud_rate,
                    timeout=serial_rx_timeout)
    serial.reset_input_buffer()
    serial.send_break(wake_break_duration)
    time.sleep(wake_settle_delay)
    serial.reset_input_buffer()
    serial.close()


def dfu():
    wake_nrf()
    print("Sending USOCK DFU request...")
    serial_request(encode_usock(cbor_dfu) + slip_end)
    time.sleep(reboot_delay)


parser = argparse.ArgumentParser()
parser.add_argument("-p", "--port", required=True)
parser.add_argument("--dfu", choices=["usock"], required=True)
args = parser.parse_args()

dfu()
