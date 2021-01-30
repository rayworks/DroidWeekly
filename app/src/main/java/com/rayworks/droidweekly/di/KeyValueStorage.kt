package com.rayworks.droidweekly.di

import androidx.annotation.NonNull

interface KeyValueStorage {
    fun putString(key: String, @NonNull value: String)
    fun putInt(key: String, value: Int)
    fun getInt(key: String, defValue: Int = 0): Int
    fun getString(key: String, defValue: String = ""): String
}