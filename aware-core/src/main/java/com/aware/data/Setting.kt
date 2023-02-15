package com.aware.data

//Aware settings table has 4 columns, _id, key, value, package_name TODO package_name should not be needed
data class Setting(
    val key: String,
    val value: String
)
