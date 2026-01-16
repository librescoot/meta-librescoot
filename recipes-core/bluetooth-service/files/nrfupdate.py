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
    rx_packet = bytearray()
    while True:
        rx = serial.read()
        if len(rx) == 0:
            break
        rx_packet = rx_packet + rx
    serial.close()
    print("  Rx:", end=' ')
    print_packet(rx_packet)
    return rx_packet


def dfu():
    print("Sending USOCK DFU request...")
    serial_request(encode_usock(cbor_dfu) + slip_end)
    time.sleep(reboot_delay)


parser = argparse.ArgumentParser()
parser.add_argument("-p", "--port", required=True)
parser.add_argument("--dfu", choices=["usock"], required=True)
args = parser.parse_args()

dfu()
