package com.rayworks.droidweekly.extension

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.jsonToObject(str: String): T? {
    return try {
        fromJson(str, object : TypeToken<T>() {}.type)
    } catch (exp: JsonSyntaxException) {
        print(exp)
        null
    }
}