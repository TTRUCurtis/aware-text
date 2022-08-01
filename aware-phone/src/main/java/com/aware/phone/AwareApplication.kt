package com.aware.phone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/*
 * Required for Hilt dependency injection
 */
@HiltAndroidApp
class AwareApplication : Application()