package com.rayworks.droidweekly.repository

import android.content.Context
import android.content.SharedPreferences
import com.rayworks.droidweekly.di.KeyValueStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class AndroidKVStorage @Inject constructor(@ApplicationContext val context: Context) : KeyValueStorage {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREF_ISSUE_INFO, Context.MODE_PRIVATE)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    override fun getInt(key: String, defValue: Int): Int = preferences.getInt(key, defValue)

    override fun getString(key: String, defValue: String): String = preferences.getString(key, defValue)!!
}