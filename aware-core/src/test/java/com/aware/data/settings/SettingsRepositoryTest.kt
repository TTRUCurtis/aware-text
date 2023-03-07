package com.aware.data.settings

import org.junit.Assert.*

import org.junit.Test


/*
* TODO: Might help to write out what each test is going to test...
* Then you can see which functions are doing more than one thing
*/
class SettingsRepositoryTest {

    /*
    * Here we need to make sure of the following
    *   - if a key is passed in that *does* exist, we are getting the correct value
    *   - if a key is passed in that *doesn't* exist, we are getting "" as the value
    */
    @Test
    fun getSetting() {
    }

    /*
     * Here we need to make sure of the following:
     * if (settings not initialized)
     *  initialize settings
     * else
     *  return settings from storage
     *
     */
    @Test
    fun getSettings() {
    }

    /*
    * Here we need to make sure of the following
    *   - if we already have a device ID, throw an error
    *   - device label is done differently
    *   - otherwise, update if existing
    *   - insert if not existing
    */
    @Test
    fun setSettingInStorage() {
    }
}