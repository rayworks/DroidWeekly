package com.rayworks.droidweekly.di

import androidx.annotation.NonNull

/***
 * The interface for a lightweight KV storage
 */
interface KeyValueStorage {
    /**
     * Set a String value in the preferences.
     *
     * @param key The name of the preference to modify.
     * @param value The new value for the preference.
     */
    fun putString(key: String, @NonNull value: String)

    /**
     * Set an int value in the preferences.
     *
     * @param key The name of the preference to modify.
     * @param value The new value for the preference.
     */
    fun putInt(key: String, value: Int)

    /**
     * Retrieve an int value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.
     */
    fun getInt(key: String, defValue: Int = 0): Int

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return Returns the preference value if it exists, or defValue.
     */
    fun getString(key: String, defValue: String = ""): String
}