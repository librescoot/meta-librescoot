#!/usr/bin/env python3

import redis
import time
import os
from pathlib import Path
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('brightness_monitor')

# Redis configuration
REDIS_HOST = '192.168.7.1'
REDIS_PORT = 6379
REDIS_DB = 0
REDIS_KEY = 'dashboard'
REDIS_FIELD = 'brightness'

# IIO device configuration
IIO_PATH = '/sys/bus/iio/devices'

def find_opt3001_device():
    """Find the OPT3001 light sensor in the IIO subsystem."""
    try:
        for device_dir in Path(IIO_PATH).glob('iio:device*'):
            name_path = device_dir / 'name'
            if name_path.exists():
                with open(name_path, 'r') as f:
                    if 'opt3001' in f.read().lower():
                        return device_dir
        return None
    except Exception as e:
        logger.error(f"Error finding OPT3001 device: {e}")
        return None

def read_brightness(device_path):
    """Read the brightness value from the OPT3001 sensor."""
    try:
        # For OPT3001, we need to read the processed value
        in_illuminance_path = device_path / 'in_illuminance_input'
        
        # If the path doesn't exist, try alternative paths
        if not in_illuminance_path.exists():
            # Try in_intensity_input for OPT3002
            in_illuminance_path = device_path / 'in_intensity_input'
            
            # If still doesn't exist, look for any in_*_input file
            if not in_illuminance_path.exists():
                for p in device_path.glob('in_*_input'):
                    in_illuminance_path = p
                    break
        
        with open(in_illuminance_path, 'r') as f:
            # The value is usually in millilux or similar scaled units
            raw_value = f.read().strip()
            
            # Convert to float, might need scaling depending on the exact format
            return float(raw_value)
    except Exception as e:
        logger.error(f"Error reading brightness: {e}")
        return None

def main():
    # Find the OPT3001 device
    device_path = find_opt3001_device()
    if not device_path:
        logger.error("Could not find OPT3001 device. Exiting.")
        return

    logger.info(f"Found OPT3001 device at {device_path}")

    # Connect to Redis
    try:
        r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=REDIS_DB)
        r.ping()  # Test connection
        logger.info("Connected to Redis successfully")
    except redis.ConnectionError as e:
        logger.error(f"Failed to connect to Redis: {e}")
        return

    # Main loop
    logger.info("Starting brightness monitoring loop")
    try:
        while True:
            # Read brightness
            brightness = read_brightness(device_path)
            
            if brightness is not None:
                # Store in Redis
                r.hset(REDIS_KEY, REDIS_FIELD, str(brightness))
                logger.info(f"Stored brightness: {brightness}")
            else:
                logger.warning("Failed to read brightness")
            
            # Wait for 1 second
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Monitoring stopped by user")
    except Exception as e:
        logger.error(f"Unexpected error: {e}")

if __name__ == "__main__":
    main()
