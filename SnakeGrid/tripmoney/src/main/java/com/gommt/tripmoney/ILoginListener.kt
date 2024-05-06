package com.gommt.tripmoney

interface ILoginListener {
    fun onLoginStateChanged(loginState: LoginState, auth: String)
}

enum class LoginState{
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT
}