package com.gommt.tripmoney.model

import java.net.URI
import java.util.*

/**
 * Cookie class created to contain the properties of a cookie
 */
data class Cookie(var cookieName: String?,
                  var cookieValue: String?
) {
    var path: String? = null
    var domain: String? = null
    var expiryDate: Date? = null

    constructor(cookieName: String?,
                cookieValue: String?,
                path: String? = null,
                domain: String? = null,
                expiryDate: Date? = null): this(cookieName, cookieValue) {
        this.path = path
        this.domain = URI(domain).host
        this.expiryDate = expiryDate
    }

    fun getCookieString(): String{
        val builder = StringBuilder()
        addParamToCookieString(builder, cookieName, cookieValue)
        addParamToCookieString(builder, COOKIE_PARAM_PATH, path)
        addParamToCookieString(builder, COOKIE_PARAM_DOMAIN, domain)
        if (expiryDate != null) {
            addParamToCookieString(builder, COOKIE_PARAM_EXPIRY_DATE, expiryDate.toString())
        }
        return builder.toString()
    }

    companion object {
        fun addParamToCookieString(builder: StringBuilder, paramName: String?, paramValue: String?): StringBuilder {
            if (paramName != null && paramValue != null) {
                builder.append(paramName).append(EQUAL_SIGN).append(paramValue).append(SEMICOLON_DELIMITER)
            }
            return builder
        }

        fun getCookieString(cookieList: List<Cookie>): String {
            val stringBuilder = StringBuilder()
            for (cookie in cookieList) {
                addParamToCookieString(stringBuilder, cookie.cookieName, cookie.cookieValue)
            }
            return stringBuilder.toString()
        }

        private const val COOKIE_PARAM_PATH = "path"
        private const val COOKIE_PARAM_DOMAIN = "domain"
        private const val COOKIE_PARAM_EXPIRY_DATE = "expires"
        private const val EQUAL_SIGN = "="
        private const val SEMICOLON_DELIMITER = "; "
    }

}