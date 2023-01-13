AWARE Plugin: Google Login
===================================

This plugin allows researchers and users to personalise their AWARE experience with Google.

# Settings
Parameters adjusted on the dashboard and client:
* **status_plugin_google_login**: (boolean) activate/deactivate plugin

# Broadcasts
**ACTION_AWARE_GOOGLE_LOGIN_COMPLETE**
Broadcasted when the user logs in successfully, with the following extras:
- **google_account**: (ContentValues) users' profile information
    
# Providers
##  Google Profile Data
> content://com.aware.plugin.google.login.provider.google_login/plugin_google_login

Field | Type | Description
----- | ---- | -----------
_id | INTEGER | primary key auto-incremented
timestamp | REAL | unix timestamp in milliseconds of sample
device_id | TEXT | AWARE device ID
name | TEXT | users' full name
email | TEXT | users' email
phonenumber | TEXT | users' phone number (if available)
blob_picture | BLOB | users' profile picture