package com.gommt.tripmoney.storage

import android.content.Context
import android.content.SharedPreferences


class WebViewKeyValueStorage(private val preferences: SharedPreferences) : Storage {
    companion object {
        const val WEBVIEW_KEYS = "web_view_keys"
        const val SHARED_PREF_NAME = "web_view_prefs"

        lateinit var prefs: WebViewKeyValueStorage

        fun getInstance(context: Context): WebViewKeyValueStorage {
            if (!(::prefs.isInitialized)) {
                prefs = WebViewKeyValueStorage(
                    context.getSharedPreferences(
                        SHARED_PREF_NAME,
                        Context.MODE_PRIVATE
                    )
                )
            }
            return prefs
        }
    }

    override fun getStorageValue(key: String, default: String?): String? {
        return preferences.getString(key, default) ?: default
    }

    override fun setStorageValue(key: String, value: String) {
        preferences.edit().putString(key,value).commit()
    }

    override fun clear(key: String) {
        preferences.edit().remove(key).commit()
    }

    override fun clearAll() {
        preferences.edit().clear().commit()
    }

}