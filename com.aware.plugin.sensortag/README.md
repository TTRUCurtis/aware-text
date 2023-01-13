AWARE Plugin: SensorTag
==========================

[![Release](https://jitpack.io/v/denzilferreira/com.aware.plugin.sensortag.svg)](https://jitpack.io/#denzilferreira/com.aware.plugin.sensortag)

This plugin receives data from the Texas Instruments' SensorTag.

# Settings
Parameters adjustable on the dashboard and client:
- **status_plugin_sensortag**: (boolean) enable/disable plugin
- **frequency_plugin_sensortag**: (integer) Data Collection Frequency (Default 10Hz)

# Providers
##  Device Usage Data
> content://com.aware.plugin.sensortag.provider.sensortag/sensortag

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
update_period | TEXT | Frequency at which sensor collects data
sensor| TEXT | One of Accelerometer, Gyro, Magnetometer, Humidity, Light or Pressure
value | REAL | Value from the sensor
unit | TEXT | Unit of the reading
