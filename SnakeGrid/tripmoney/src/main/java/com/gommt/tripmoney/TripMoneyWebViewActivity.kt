package com.gommt.tripmoney

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.webkit.WebView.setWebContentsDebuggingEnabled
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gommt.tripmoney.storage.Storage
import com.gommt.tripmoney.storage.WebViewKeyValueStorage
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import com.gommt.tripmoney.BuildConfig


class TripMoneyWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var url: String
    private lateinit var tempLocationCallback: String
    private lateinit var tempCameraCallback: String
    private lateinit var tempGalleryCallback: String
    private var tempGalleryValueCallback: ValueCallback<Array<Uri>>? = null
    private var tempFileChooserParams: WebChromeClient.FileChooserParams? = null
    private lateinit var tripMoneyState: State<TripMoneyState>
    private var cameraUri: Uri? = null
    private var headers = HashMap<String, String>()
    private var isShown = false
    private lateinit var cameraPermissionRequestLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    private lateinit var locationPermissionRequestLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    private lateinit var storage: Storage

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                if (result.data?.data !=null ){
                    result.data?.data?.also { uri ->
                        if (tempGalleryValueCallback != null) {
                            cameraUri?.let {
                                tempGalleryValueCallback!!.onReceiveValue(arrayOf(uri))
                            }
                        } else {
                            if (this::tempGalleryCallback.isInitialized) {
                                imageCallback(tempGalleryCallback, uri.toString())
                            }
                        }
                    }
                } else {
                    if (tempGalleryValueCallback != null) {
                        cameraUri?.let {
                            tempGalleryValueCallback!!.onReceiveValue(arrayOf(it))
                        }
                    } else {
                        if (this::tempGalleryCallback.isInitialized) {
                            imageCallback(tempGalleryCallback, cameraUri.toString())
                        }
                    }
                }
            } else if (this::tempGalleryCallback.isInitialized) {
                imageCallback(tempGalleryCallback, null)
            } else if (tempGalleryValueCallback != null) {
                tempGalleryValueCallback!!.onReceiveValue(null)
            }
            tempGalleryValueCallback = null
            tempFileChooserParams = null
            cameraUri = null
        }

    val paymentStatus = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            finish()
        }
    }

    val loginStateListener = object : ILoginListener {
        override fun onLoginStateChanged(loginState: LoginState, auth: String) {
            val action = when (loginState) {
                LoginState.LOGIN_SUCCESS -> onLoginSuccess(auth)
                LoginState.LOGIN_FAILED -> onLoginCancelled()
                LoginState.LOGOUT -> onLogout()
            }
            webView.loadUrl(action)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold {
                Box(
                    Modifier
                        .padding(it)
                        .fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    val tripMoneyViewModel = viewModel { TripMoneyWebViewViewModel() }
                    OnPermissionResult()
                    tripMoneyState = remember {
                        mutableStateOf(tripMoneyViewModel.tripMoneyState)
                    }.value

                    lifecycle.addObserver(object : LifecycleEventObserver{
                        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                            tripMoneyImpl?.getLifeCycleCallbacks(event)
                        }
                    })

                    setData(tripMoneyState)
                    val activity = (LocalContext.current as? ComponentActivity)
                    if (activity != null) {
                        MainContent(tripMoneyState = tripMoneyState.value, context = activity)
                    }
                }
            }
        }
        storage = WebViewKeyValueStorage.getInstance(this@TripMoneyWebViewActivity)
    }

    private fun retrieveInsuranceRedirectUrl(paymentResponse: String?): String? {
        if(paymentResponse == null){
            return null
        }
        var redirectUrl:String? = null
        try {
            val paymentResponseJSON = JSONObject(paymentResponse)
            val response  = paymentResponseJSON.getString("response")
            val responseJSON = JSONObject(response)
            response.let {
                responseJSON.getString("redirectUrl").let { redirect ->
                    redirectUrl = redirect
                }
            }
        } catch (e: Exception) {
        }

        return redirectUrl

    }
    private fun setData(tripMoneyState: State<TripMoneyState>) {
        if (BuildConfig.DEBUG) {
            setWebContentsDebuggingEnabled(true)
        }
        tripMoneyState.value.isProgressBarBlocking =
            intent.getBooleanExtra(EXTRA_BLOCKING_PROGRESS_BAR, true)
        tripMoneyState.value.withLogin =
            intent.getBooleanExtra(EXTRA_LOGIN_SUPPORTED, false)
        tripMoneyState.value.toolBarVisibility =
            intent.getBooleanExtra(EXTRA_TOOLBAR_VISIBILITY, true)
        tripMoneyState.value.enableClose =
            intent.getBooleanExtra(EXTRA_TOOLBAR_ENABLE_CLOSE, false)

        if (intent.hasExtra(EXTRA_URL)) {
            intent.extras?.getString(EXTRA_URL)?.let { url ->
                this.url = url
                tripMoneyState.value.mUrl = url
            }
        }else if(intent.hasExtra(KEY_MMT_PAYMENT_RESPONSE)){
            //TODO this handling is specific to Makemytrip. This need to be removed in future versions.
           val redirectUrl =  retrieveInsuranceRedirectUrl(intent.getStringExtra(KEY_MMT_PAYMENT_RESPONSE))
            if(redirectUrl == null){
                finish()
                return
            }else{
                tripMoneyState.value.toolBarVisibility = false
                this.url = redirectUrl
                tripMoneyState.value.mUrl = redirectUrl
            }
        }else if(intent.hasExtra(KEY_GI_PAYMENT_RESPONSE)){
            //TODO this handling is specific to GI. This need to be removed in future versions.
            val redirectUrl =  retrieveInsuranceRedirectUrl(intent.getStringExtra(KEY_GI_PAYMENT_RESPONSE))
            if(redirectUrl != null){
                tripMoneyState.value.toolBarVisibility = false
                this.url = redirectUrl
                tripMoneyState.value.mUrl = redirectUrl
            }
        } else {
            finish()
        }

        if (intent.hasExtra(TITLE)) {
            intent.extras?.getString(TITLE)?.let { title ->
                tripMoneyState.value.title = title
            }
        } else {
            tripMoneyState.value.title = getString(R.string.webview_default_title)
        }

        if (intent.hasExtra(TOOLBAR_COLOR)) {
            tripMoneyState.value.colorTheme.bgColor =
                Color(intent.getIntExtra(TOOLBAR_COLOR, Color.White.toArgb()))
        }

        if (intent.hasExtra(TEXT_COLOR)) {
            tripMoneyState.value.colorTheme.textColor =
                Color(intent.getIntExtra(TEXT_COLOR, Color.Black.toArgb()))
        }
        fetchLocation()
        addCookiesToWebView()
        addHeadersToWebView()
    }

    private fun addCookiesToWebView() {
        tripMoneyImpl?.addCookieList { cookieList ->
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                if (cookieList.isEmpty()) return@addCookieList
                for (cookie in cookieList) {
                    val cookieDomain: String = cookie.domain?: URI(url).host
                    cookieManager.setCookie(cookieDomain, cookie.getCookieString())
                    cookieManager.setCookie(URI(url).host, cookie.getCookieString())
                }
            } catch (_: java.lang.Exception) {
            }
        }
    }

    private fun addHeadersToWebView() {
        tripMoneyImpl?.addHeaders { headers ->
            if(headers != null){
                this.headers.putAll(headers)
            }
        }
    }

    private fun fetchLocation(){
        tripMoneyImpl?.getLocation(
            context = this@TripMoneyWebViewActivity,
            isPermissionNeeded = false,
            locationPermissionRequestLauncher
        ) { }
    }

    private fun mWebViewClient(
        tripMoneyState: TripMoneyState,
        context: Activity,
        tripMoneyViewModel: TripMoneyWebViewViewModel
    ): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (isExternal(url)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                    return true
                }

                if (tripMoneyImpl?.canOpenInNative(url) == true) {
                    tripMoneyViewModel.setProgress(false)
                    var modifiedUrl = url
                    if (url.contains("goibibo:")) {
                        modifiedUrl = url.replace("goibibo:", "")
                    }
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = Uri.parse(modifiedUrl)
                    intent.data = uri
                    tripMoneyImpl?.checkAndLaunchDeeplinkActivity(intent, context)
                } else {
                    if (!tripMoneyViewModel.loadingFinished.value) {
                        tripMoneyViewModel.setRedirect(true)
                    }
                    tripMoneyViewModel.setLoading(false)

                    view.loadUrl(url)
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                tripMoneyViewModel.setLoading(false)
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!tripMoneyViewModel.redirect.value) {
                    tripMoneyViewModel.setLoading(true)
                }
                tripMoneyState.count++
                if (tripMoneyState.count > 2) {
                    tripMoneyViewModel.setProgress(false)
                }
                if (tripMoneyViewModel.loadingFinished.value && !tripMoneyViewModel.redirect.value) {
                    // HIDE LOADING IT HAS FINISHED
                    tripMoneyViewModel.setProgress(false)
                } else {
                    tripMoneyViewModel.setRedirect(false)
                }
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                if (tripMoneyImpl?.isInternetConnected(context = this@TripMoneyWebViewActivity) == false) {
                    tripMoneyViewModel.setNoInternetDialog(true)

                    tripMoneyImpl?.showError(
                        context = context,
                        context.getString(R.string.cf_no_net_title)
                    )
                } else tripMoneyImpl?.showError(
                    context = context,
                    "Unable to connect to server please try after sometime."
                )
            }
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun MainContent(tripMoneyState: TripMoneyState, context: ComponentActivity) {
        Scaffold(topBar = {
            if (tripMoneyState.toolBarVisibility) {
                InitToolBar(tripMoneyState, context = context)
            }
        }, content = { MyContent(tripMoneyState = tripMoneyState, context = context) })
    }

    @Composable
    fun InitToolBar(tripMoneyState: TripMoneyState, context: ComponentActivity) {
        val bgColor: Color = tripMoneyState.colorTheme.bgColor
        val textColor: Color = tripMoneyState.colorTheme.textColor

        TopAppBar(
            title = { Text(tripMoneyState.title, color = textColor) },
            backgroundColor = bgColor,
            navigationIcon = {
                IconButton(onClick = {
                    context.onBackPressedDispatcher.onBackPressed()
                }) {
                    Icon(
                        imageVector = if (tripMoneyState.enableClose) {
                            Icons.Filled.Close
                        } else {
                            Icons.Filled.ArrowBack
                        },
                        tint = textColor,
                        contentDescription = "Toolbar back button"
                    )
                }
            },
        )
    }

    @Composable
    fun ProgressBar(tripMoneyState: TripMoneyState) {
        val tripMoneyViewModel = viewModel { TripMoneyWebViewViewModel() }
        if (tripMoneyViewModel.shouldShowProgress.value) {
            if (tripMoneyState.isProgressBarBlocking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(5f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color.Transparent,
                )
            }
        }
    }

    @Composable
    fun MyContent(tripMoneyState: TripMoneyState, context: ComponentActivity) {
        Column {
            ProgressBar(tripMoneyState)
            NoInternetView()
            CustomWebView(tripMoneyState = tripMoneyState, context = context)
        }
    }

    @Composable
    fun NoInternetView() {
        val tripMoneyViewModel = viewModel { TripMoneyWebViewViewModel() }
        if (tripMoneyViewModel.shouldShowNoInternetDialog.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .zIndex(5f),
            )
        }
    }

    private fun locationUpdate(callback: String, location: Location?) {
        if (location != null) {
            val locationString =
                "{ Latitude:" + location.latitude + ", Longitude:" + location.longitude + "}"
            sendJSONData(callback, JSONObject(locationString))
        } else {
            sendJSONData(callback, null)
        }
    }

    private fun imageCallback(callback: String, image: String?) {
        if (image!=null){
            sendStringData(callback, image)
        } else {
            sendStringData(callback, null)
        }
    }

    private fun sendJSONData(callback: String, jsonObject: JSONObject?) {
        val function = String.format(
            JS_1ARG_FUNC_SIGN,
            callback,
            jsonObject?.encode()
        )
        if (this::webView.isInitialized) {
            webView.post {
                webView.loadUrl(function)
            }
        }
    }

    private fun sendStringData(callback: String, string: String?) {
        val function = String.format(
            JS_1ARG_FUNC_SIGN,
            callback,
            string
        )
        if (this::webView.isInitialized) {
            webView.post {
                webView.loadUrl(function)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun CustomWebView(tripMoneyState: TripMoneyState, context: ComponentActivity) {
        val tripMoneyViewModel = viewModel { TripMoneyWebViewViewModel() }

        val tripMoneyInterface: Any = object {
            @JavascriptInterface
            fun closePage() {
                finish()
            }

            @JavascriptInterface
            fun closeWebView() {
                finish()
            }

            @JavascriptInterface
            fun closeAndRefreshWebView() {
                finish()
            }

            @JavascriptInterface
            fun showProgress() {
                tripMoneyViewModel.setProgress(true)
            }

            @JavascriptInterface
            fun hideProgress() {
                tripMoneyViewModel.setProgress(false)
            }

            @JavascriptInterface
            fun sendEvent(nativeData: String?) {
                if (!isNullOrWhiteSpace(nativeData)) {
                    try {
                        val nativeDataObj = nativeData?.let { JSONObject(it) }
                        nativeDataObj?.let {
                            tripMoneyImpl?.fireEvent(
                                context,
                                url,
                                it
                            )
                        }
                    } catch (_: JSONException) {
                    }
                }
            }

            @JavascriptInterface
            fun getAppVersion(): String? {
                return tripMoneyImpl?.getAppVersionName(this@TripMoneyWebViewActivity)
            }

            @JavascriptInterface
            fun fetchLocation(callback: String, shouldBlock: Boolean = true) {
                tempLocationCallback = callback
                tripMoneyImpl?.getLocation(
                    context = this@TripMoneyWebViewActivity,
                    isPermissionNeeded = shouldBlock,
                    locationPermissionRequestLauncher
                ) { location ->
                    if (this@TripMoneyWebViewActivity::tempLocationCallback.isInitialized) {
                        locationUpdate(tempLocationCallback, location)
                    }
                }
            }

            @JavascriptInterface
            fun openCamera(callback: String) {
                cameraUri = getPhotoFileUri(this@TripMoneyWebViewActivity)
                isShown = false
                try {
                    tempCameraCallback = callback
                    tripMoneyImpl?.openCamera(
                        this@TripMoneyWebViewActivity,
                        galleryLauncher,
                        cameraUri!!,
                        cameraPermissionRequestLauncher
                    )
                } catch (_: java.lang.Exception) {
                }
            }


            @JavascriptInterface
            fun openGallery(callback: String) {
                cameraUri = getPhotoFileUri(this@TripMoneyWebViewActivity)
                isShown = false
                try {
                    tempGalleryCallback = callback
                    tripMoneyImpl?.openGallery(
                        this@TripMoneyWebViewActivity,
                        galleryLauncher,
                        cameraUri!!
                    )
                } catch (_: java.lang.Exception) {
                }
            }

            @JavascriptInterface
            fun navigateToSettings() {
                tripMoneyImpl?.navigateToSettings(context)
            }

            @JavascriptInterface
            fun requestDownload(fileURL: String) {
                tripMoneyImpl?.downloadPdf(
                    context = context,
                    url = fileURL,
                    fileName = "file"
                )
            }

            @JavascriptInterface
            fun shareUrl(shareMessage: String, imageUrl: String) {
                tripMoneyImpl?.shareMessage(
                    context = context,
                    shareMessage = shareMessage,
                    imageUrl = imageUrl,
                    cameraPermissionRequestLauncher
                )
            }


            @JavascriptInterface
            fun copyToClipboard(textToCopy: String) {
                tripMoneyImpl?.copyToClipboard(
                    context = context,
                    textToCopy = textToCopy
                )
            }

            @JavascriptInterface
            fun initiatePayment(paymentInfo: String?) {
                paymentInfo?.let {
                    tripMoneyImpl?.initiatePayment(it) { intent ->
                        paymentStatus.launch(intent)
                    }
                }
            }

            @JavascriptInterface
            fun login(loginParams: String) {
                tripMoneyImpl?.login(this@TripMoneyWebViewActivity, loginParams, loginStateListener)
            }

            @JavascriptInterface
            fun logoutUser() {
                tripMoneyImpl?.logout(loginStateListener)
            }


            @JavascriptInterface
            fun getUserLoginStatus(): Boolean {
                return tripMoneyImpl?.getLoginStatus() ?: false
            }

            @JavascriptInterface
            fun saveTMToken(token: String) {
                tripMoneyImpl?.saveTripMoneyToken(token)
            }

            @JavascriptInterface
            fun getTMToken(): String {
                return tripMoneyImpl?.getTripMoneyToken() ?: ""
            }

            @JavascriptInterface
            fun getShortLivToken(request: String, callback: String) {
                tripMoneyImpl?.getShortLivedTokenForTripMoney(request,
                    object : ShortLivedTokenCallback {
                        override fun shareShortLivedToken(shortLivedToken: String) {
                            var jsMethodCall = "javascript:"
                            jsMethodCall += callback
                            jsMethodCall += "('"
                            jsMethodCall += shortLivedToken
                            jsMethodCall += "')"
                            webView.loadUrl(jsMethodCall)
                        }
                    })
            }

            @JavascriptInterface
            fun deviceId(): String {
                return getDeviceId(this@TripMoneyWebViewActivity)
            }

            @JavascriptInterface
            fun openDeeplink(link: String) {
                tripMoneyImpl?.openDeeplink(link, context)
            }

            @JavascriptInterface
            fun getUserConsent(): String? {
                return tripMoneyImpl?.getUserConsent()
            }

            @JavascriptInterface
            fun getInAppStorage(key:String,default:String?) : String?{
               return storage.getStorageValue(key,default)
            }

            @JavascriptInterface
            fun getInAppStorage(key:String) : String?{
                return storage.getStorageValue(key,null)
            }

            @JavascriptInterface
            fun setInAppStorage(key:String,value:String){
                storage.setStorageValue(key,value)
            }

            @JavascriptInterface
            fun clearKey(key:String){
                storage.clear(key)
            }

            @JavascriptInterface
            fun clearAll(key:String){
                storage.clearAll()
            }

            @JavascriptInterface
            fun getUserProfileType() : String{
                return tripMoneyImpl?.getUserProfileType() ?: Constants.DEFAULT_PROFILE_TYPE
            }
        }
        val lendingWebChromeClient: WebChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                tempGalleryValueCallback = filePathCallback
                tempFileChooserParams = fileChooserParams
                cameraUri = getPhotoFileUri(this@TripMoneyWebViewActivity)
                isShown = false
                if (isCameraPermissionGranted(this@TripMoneyWebViewActivity, cameraPermissionRequestLauncher)) {
                    cameraUri?.let {
                        openImageUploadChooser(true, it, galleryLauncher, tempFileChooserParams)
                    }
                }
                return true
            }

            override fun onPermissionRequest(permissionRequest: PermissionRequest) {
                isShown = false
                isCameraPermissionGranted(this@TripMoneyWebViewActivity, cameraPermissionRequestLauncher)
            }
        }
        AndroidView(factory = {
            webView = WebView(it)
            webView.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                clearHistory()
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = true
                requestFocusFromTouch()
                setInitialScale(1)
                settings.useWideViewPort = true
                settings.javaScriptEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.builtInZoomControls = true
                settings.supportZoom()
                settings.pluginState = WebSettings.PluginState.ON
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                if (BuildConfig.DEBUG) {
                    setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                }

                addJavascriptInterface(tripMoneyInterface, "mmt_android_bridge")
                addJavascriptInterface(tripMoneyInterface, "app_bridge")
                webChromeClient = lendingWebChromeClient
                /**
                 * added handling to handle downloadable file specially added to handle .pdf file
                 * and would be used for media files in links
                 */
                setDownloadListener { url, _, _, _, _ ->
                    try {
                        val i = Intent(Intent.ACTION_VIEW)
                        i.data = Uri.parse(url)
                        context.startActivity(i)
                    } catch (_: Exception) {
                    }
                }
                webViewClient = mWebViewClient(
                    tripMoneyState = tripMoneyState,
                    context = context,
                    tripMoneyViewModel
                )
                loadUrl(tripMoneyState.mUrl, headers)
            }
        }, update = {
            it.loadUrl(tripMoneyState.mUrl, headers)
        })

        onBackPressedDispatcher.addCallback {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }
    }

    @Composable
    private fun OnPermissionResult() {
        cameraPermissionRequestLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val allPermissionsGranted = permissions.values.any { it }

            if (allPermissionsGranted) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    if (tempGalleryValueCallback != null) {
                        cameraUri?.let {
                            openImageUploadChooser(
                                true,
                                it,
                                galleryLauncher,
                                tempFileChooserParams
                            )
                        }
                    } else {
                        cameraUri?.let {
                            tripMoneyImpl?.openCamera(
                                this@TripMoneyWebViewActivity,
                                galleryLauncher,
                                it,
                                cameraPermissionRequestLauncher
                            )
                        }
                    }
                } else {
                    if (tempGalleryValueCallback != null) {
                        cameraUri?.let {
                            openImageUploadChooser(
                                false,
                                it,
                                galleryLauncher,
                                tempFileChooserParams
                            )
                        }
                    } else {
                        showSettingsSnackBar(this@TripMoneyWebViewActivity)
                        if (this::tempCameraCallback.isInitialized) {
                            imageCallback(tempCameraCallback, null)
                        }
                        cameraUri = null
                        tempGalleryValueCallback?.onReceiveValue(null)
                        tempGalleryValueCallback = null
                        tempFileChooserParams = null
                        isShown = true
                    }
                }
            } else {
                showSettingsSnackBar(this@TripMoneyWebViewActivity)
                if (this::tempCameraCallback.isInitialized) {
                    imageCallback(tempCameraCallback, null)
                }
                cameraUri = null
                tempGalleryValueCallback?.onReceiveValue(null)
                tempGalleryValueCallback = null
                tempFileChooserParams = null
                isShown = true
            }
        }
        locationPermissionRequestLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val allPermissionsGranted = permissions.values.all { it }

            if (allPermissionsGranted) {
                tripMoneyImpl?.getLocation(
                    context = this@TripMoneyWebViewActivity,
                    permissionLauncher = locationPermissionRequestLauncher
                ) { location ->
                    if (this::tempLocationCallback.isInitialized) {
                        locationUpdate(tempLocationCallback, location)
                    }
                }
            } else {
                if (this::tempLocationCallback.isInitialized) {
                    locationUpdate(tempLocationCallback, null)
                }
            }
        }
    }

    private fun onLoginSuccess(auth: String) = "javascript:onLoginSuccess('$auth')"
    private fun onLoginCancelled() = "javascript:onLoginCancelled()"
    private fun onLogout() = "javascript:logoutUser()"

    companion object {
        const val KEY_MMT_PAYMENT_RESPONSE = "PAYMENT_RESPONSE_VO"
        const val KEY_GI_PAYMENT_RESPONSE = "lob_response_payload"
        private const val EXTRA_BLOCKING_PROGRESS_BAR = "blocking_progress"
        private const val EXTRA_TOOLBAR_VISIBILITY = "toolbar"
        private const val EXTRA_TOOLBAR_ENABLE_CLOSE = "show_cross"
        private const val EXTRA_LOGIN_SUPPORTED = "loginSupported"
        private const val EXTRA_URL = "url"
        private const val TITLE = "title"
        private const val TOOLBAR_COLOR = "toolbar_color"
        private const val TEXT_COLOR = "text_color"
        private const val JS_1ARG_FUNC_SIGN = "javascript:%s('%s')"
        private var tripMoneyImpl: ITripMoneyWebView? = null

        fun builder(
            context: Context,
            url: String,
            tripMoneyImpl: ITripMoneyWebView
        ): TripMoneyWebViewBuilder {
            this.tripMoneyImpl = tripMoneyImpl
            return TripMoneyWebViewBuilder(context, url)
        }

        class TripMoneyWebViewBuilder(private val context: Context, private val url: String) {
            private var title: String = ""
            private var toolbarColor: Int = 0
            private var textColor: Int = 0
            private var shouldShowBlockingProgressBar: Boolean = true
            private var shouldShowToolbar: Boolean = true
            private var toolbarCloseButton: Boolean = false
            private var loginSupport: Boolean = false

            fun setTitle(title: String): TripMoneyWebViewBuilder {
                this.title = title
                return this
            }

            fun setToolbarColor(toolbarColor: Int): TripMoneyWebViewBuilder {
                this.toolbarColor = toolbarColor
                return this
            }

            fun setTextColor(textColor: Int): TripMoneyWebViewBuilder {
                this.textColor = textColor
                return this
            }

            fun setShouldShowBlockingProgressBar(shouldShowBlockingProgressBar: Boolean): TripMoneyWebViewBuilder {
                this.shouldShowBlockingProgressBar = shouldShowBlockingProgressBar
                return this
            }

            fun setShouldShowToolbar(shouldShowToolbar: Boolean): TripMoneyWebViewBuilder {
                this.shouldShowToolbar = shouldShowToolbar
                return this
            }

            fun setToolbarCloseButton(toolbarCloseButton: Boolean): TripMoneyWebViewBuilder {
                this.toolbarCloseButton = toolbarCloseButton
                return this
            }

            fun setLoginSupport(loginSupport: Boolean): TripMoneyWebViewBuilder {
                this.loginSupport = loginSupport
                return this
            }

            fun build(): Intent {
                val intent = Intent(context, TripMoneyWebViewActivity::class.java)
                intent.putExtra(EXTRA_URL, url)
                intent.putExtra(TITLE, title)
                intent.putExtra(TOOLBAR_COLOR, toolbarColor)
                intent.putExtra(TEXT_COLOR, textColor)
                intent.putExtra(EXTRA_BLOCKING_PROGRESS_BAR, shouldShowBlockingProgressBar)
                intent.putExtra(EXTRA_TOOLBAR_VISIBILITY, shouldShowToolbar)
                intent.putExtra(EXTRA_TOOLBAR_ENABLE_CLOSE, toolbarCloseButton)
                intent.putExtra(EXTRA_LOGIN_SUPPORTED, loginSupport)
                return intent
            }
        }
    }
}