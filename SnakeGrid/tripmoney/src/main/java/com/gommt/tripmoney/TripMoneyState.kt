package com.gommt.tripmoney

import androidx.compose.ui.graphics.Color

data class TripMoneyState(
    var isProgressBarBlocking: Boolean = true,
    var withLogin: Boolean = false,
    var toolBarVisibility: Boolean = true,
    var enableClose: Boolean = false,
//    var tripMoneyImpl: ITripMoneyWebView? = null,
    var mUrl: String = "",
    var title: String = "",
    var count: Int = 0,
    var colorTheme: ColorTheme = ColorTheme(bgColor = Color.White, textColor = Color.Black),
)