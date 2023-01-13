AWARE Plugin: Google Fused Location
===================================

[![Release](https://jitpack.io/v/denzilferreira/com.aware.plugin.google.fused_location.svg)](https://jitpack.io/#denzilferreira/com.aware.plugin.google.fused_location)

This plugin uses Google's Fused Locations API to provide the user's current location in an energy efficient way. It also allows the user to define geo-tagged areas.

# Settings
Parameters adjusted on the dashboard and client:
* **status_google_fused_location**: (boolean) activate/deactivate plugin
* **frequency_google_fused_location**: (integer) How frequently to fetch user's location (in seconds), default 300 seconds
* **max_frequency_google_fused_location**: (integer) How fast are you willing to get the latest location (in seconds), default 60 seconds
* **accuracy_google_fused_location**: (integer) One of the following:
    * 100 (high power): uses GPS only - works best outdoors, highest accuracy
    * 102 (balanced): uses GPS, Network and Wifi - works both indoors and outdoors, good accuracy (default)
    * 104 (low power): uses only Network and WiFi - poorest accuracy, medium accuracy
    * 105 (no power) - scavenges location requests from other apps
* **fallback_location_timeout**: (integer) wait X seconds for GPS satellite fix to timeout, default 20 seconds
* **location_sensitivity**: (integer) move more than X meter(s) to request another location fix, default 5 meters

# Broadcasts
**ACTION_AWARE_LOCATIONS**
Broadcasted when we have a new location, with the following extras:
- **data**: (Location) latest location information

**ACTION_AWARE_PLUGIN_FUSED_ENTERED_GEOFENCE**
Broadcasted when we are inside a geofence, with the following extras:
- **label**: (String) geofence label
- **location**: (Location) geofence location information
- **radius**: (double) geofence radius (in meters)

**ACTION_AWARE_PLUGIN_FUSED_EXITED_GEOFENCE**
Broadcasted when we left a geofence, with the following extras:
- **label**: (String) geofence label
- **location**: (Location) geofence location information
- **radius**: (double) geofence radius (in meters)
    
# Providers
##  Locations Data
> content://com.aware.provider.locations/locations

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
double_latitude | REAL | the location’s latitude, in degrees
double_longitude	| REAL | the location’s longitude, in degrees
double_bearing | REAL |	the location’s bearing, in degrees
double_speed |	REAL | the speed if available, in meters/second over ground
double_altitude | REAL | the altitude if available, in meters above sea level
provider | TEXT | gps, network, fused
accuracy | INTEGER | the estimated location accuracy
label | TEXT | Customizable label. Useful for data calibration and traceability

##  Geofences
> content://com.aware.plugin.google.fused_location.provider.geofences/fused_geofences

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
geofence_label | TEXT | user-defined label for this geofence
double_latitude | REAL | the location’s latitude, in degrees
double_longitude | REAL | the location’s longitude, in degrees
double_radius | REAL |	the geofence radius (in meters)

##  Geofences Data
> content://com.aware.plugin.google.fused_location.provider.geofences/fused_geofences_data

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
geofence_label | TEXT | user-defined label for this geofence
double_latitude | REAL | the location’s latitude, in degrees
double_longitude | REAL | the location’s longitude, in degrees
double_distance | REAL | distance between current location and geofence center (in kilometers)
status | INTEGER | 1 = entered, 0 = exited geofence