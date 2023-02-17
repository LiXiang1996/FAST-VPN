package com.example.test.base.utils

import android.content.Context
import android.content.SharedPreferences


object SharedPreferencesUtils {
    private const val FILE_NAME = "share_date"
    fun setParam(context: Context, key: String?, value: Any) {
        val type = value.javaClass.simpleName
        val sp: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val editor = sp.edit()
        when (type) {
            "String" -> {
                editor.putString(key, value as String)
            }
            "Integer" -> {
                editor.putInt(key, (value as Int))
            }
            "Boolean" -> {
                editor.putBoolean(key, (value as Boolean))
            }
            "Float" -> {
                editor.putFloat(key, (value as Float))
            }
            "Long" -> {
                editor.putLong(key, (value as Long))
            }
        }
        editor.apply()
    }

    fun getParam(context: Context, key: String?, defaultObject: Any): Any? {
        val type = defaultObject.javaClass.simpleName
        val sp: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        when (type) {
            "String" -> {
                return sp.getString(key, defaultObject as String)
            }
            "Integer" -> {
                return sp.getInt(key, (defaultObject as Int))
            }
            "Boolean" -> {
                return sp.getBoolean(key, (defaultObject as Boolean))
            }
            "Float" -> {
                return sp.getFloat(key, (defaultObject as Float))
            }
            "Long" -> {
                return sp.getLong(key, (defaultObject as Long))
            }
            else -> return null
        }
    }
}