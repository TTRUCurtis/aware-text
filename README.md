AWARE Framework Android Client
======================
[![Release](https://jitpack.io/v/denzilferreira/aware-client.svg)](https://jitpack.io/#denzilferreira/aware-client)

AWARE is an Android framework dedicated to instrument, infer, log and share mobile context information,
for application developers, researchers and smartphone users. AWARE captures hardware-, software-, and 
human-based data. It encapsulates analysis, machine learning and simplifies conducting user studies 
in naturalistic and laboratory settings. For detailed information on various features of the app, setting up studies, building the app, and more, check out the documentation at [AWARE: The complete guide](https://docs.google.com/document/d/1XIYjmL2tGt5f7LdJHCCwIaQkDYNReRzG0qwjoP2_n2I/edit?usp=sharing).

![User Studies](http://www.awareframework.com/wp-content/uploads/2014/05/aware_overview1.png)

The platform is scalable with plugins and can be integrated with other platforms using JSON, MQTT or MySQL.

![Arquitecture](http://www.awareframework.com/wp-content/uploads/2015/12/aware-architecture.png)

Getting started 
===============

1 - Contributing to AWARE, building from source
===========================================

You can get the source code of all the components that make the AWARE client from GitHub.
```bash
$ git clone --recursive https://github.com/denzilferreira/aware-client.git
$ cd aware-client
$ git submodule foreach --recursive git checkout master
```

You can now import this project to Android Studio and hack away :)

2 - Using AWARE as a library in your own app
========================================

Add to the build.gradle inside your module to include AWARE's libraries

```Gradle
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    api "com.github.denzilferreira:aware-client:master-SNAPSHOT"
}
```

You can now refer to AWARE's functions inside your app. For example, if you want to use the accelerometer
sensor:

```kotlin
    Aware.startAWARE(applicationContext) //initialise core AWARE service

    Aware.setSetting(applicationContext, Aware_Preferences.FREQUENCY_ACCELEROMETER, 200000) //20Hz
    Aware.setSetting(applicationContext, Aware_Preferences.THRESHOLD_ACCELEROMETER, 0.02f) // [x,y,z] > 0.02 to log
    
    Aware.startAccelerometer(this)
    
    Accelerometer.setSensorObserver {
        val x = it.getAsDouble(Accelerometer_Provider.Accelerometer_Data.VALUES_0)
        val y = it.getAsDouble(Accelerometer_Provider.Accelerometer_Data.VALUES_1)
        val z = it.getAsDouble(Accelerometer_Provider.Accelerometer_Data.VALUES_2)
        
        println("x = $x y = $y, z = $z")
    }
```

Special sensors: Applications, Touch, Notifications, Keyboard, Crash logs
==================================================================
If you plan to leverage the Applications, Touch, Notifications, Keyboard or Crash sensors, you will need 
to obtain access to the Accessibility Services on Android OS. Create a file called "bools.xml" inside
/res/values/ inside your app: ![bools.xml](https://github.com/denzilferreira/aware-client/blob/master/aware-phone/src/main/res/values/bools.xml)


Open-source (Apache 2.0)
========================
Copyright (c) 2011 AWARE Mobile Context Instrumentation Middleware/Framework 
![https://www.awareframework.com](http://www.awareframework.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at 
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Compilation Instructions:
=========================

Clone this respository to a workspace directory
    It should contain all the necessary submodules and gradle build instructions.

Open Android Studio

    Project -> Open Project <workspace directory>
    
    Change build project from "aware-tests" to "aware-phone" (Top bar, just right of green hammer)
    Build Variant (Left Side tab column) Set build variant to desired version (debug/release)    
    Build -> Make Project
    Build -> Build Bundles -> Build APK(s)
    Completed APK located in aware_client/aware-phone/build/outpus/apk/[debug/release]/aware-phone-armeabi-[debug/release].apk
    Rename file to desired filename and move to accessble location
    
Modifying Submodules:
=====================

Each plugin has its own git repository and this main repository links to them

Currently the WWBP supports 2 plugins:

    https://github.com/wwbp/com.aware.plugin.sentiment
    https://github.com/wwbp/com.aware.plugin.sms
    
To incorporate any updates to the submodules it will require updating the version linked to in this repository. 

Updating Submodule Version in Main repository:
==============================================

```bash
    $ git clone https://github.com/wwbp/aware_client
    $ <delete submodule directory>
    $ git clone https://github.com/wwbp/<submodule>
    $ git checkout -b <branch>
    $ git diff
         Should show updated module sha
    $ git add <submodule>
    $ git commit -m "<commit message>"
    $ git push
```

