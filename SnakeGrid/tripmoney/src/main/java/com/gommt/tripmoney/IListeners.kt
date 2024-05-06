package com.gommt.tripmoney

import com.gommt.tripmoney.model.Cookie

interface IListeners {
    fun cookieAdded(cookieList: List<Cookie>)
    fun headerAdded()
}