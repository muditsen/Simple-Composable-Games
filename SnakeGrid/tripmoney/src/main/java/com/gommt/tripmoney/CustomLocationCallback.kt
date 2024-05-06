package com.gommt.tripmoney

import android.location.Location

interface CustomLocationCallback {
    fun onLocationFetched(location: Location)
}