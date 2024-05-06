package com.gommt.tripmoney

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.State

class TripMoneyWebViewViewModel : ViewModel() {

    private val _state = mutableStateOf(TripMoneyState())
    val tripMoneyState: State<TripMoneyState> = _state

    private val _loadingFinished = mutableStateOf(false)
    val loadingFinished: State<Boolean> = _loadingFinished

    private val _redirect = mutableStateOf(false)
    val redirect: State<Boolean> = _redirect

    private val _shouldShowProgress = mutableStateOf(true)
    val shouldShowProgress: State<Boolean> = _shouldShowProgress

    private val _shouldShowNoInternetDialog = mutableStateOf(false)
    val shouldShowNoInternetDialog: State<Boolean> = _shouldShowNoInternetDialog

    fun setLoading(isLoadingFinished: Boolean){
        _loadingFinished.value = isLoadingFinished
    }

    fun setProgress(shouldShowProgress: Boolean){
        _shouldShowProgress.value = shouldShowProgress
    }

    fun setNoInternetDialog(shouldShowNoInternetDialog: Boolean){
        _shouldShowNoInternetDialog.value = shouldShowNoInternetDialog
    }

    fun setRedirect(redirect: Boolean){
        _redirect.value = redirect
    }


}