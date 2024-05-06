package com.gommt.tripmoney.storage

interface Storage {

    fun getStorageValue(key: String, default: String?) : String?

    fun setStorageValue(key: String, value: String)

    fun clear(key: String)

    fun clearAll()

}