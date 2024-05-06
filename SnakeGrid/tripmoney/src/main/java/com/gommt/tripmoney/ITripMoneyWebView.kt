package com.gommt.tripmoney

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import com.gommt.tripmoney.model.Cookie
import org.json.JSONObject
import java.util.HashMap


interface ITripMoneyWebView : Parcelable {
    fun canOpenInNative(url: String): Boolean
    fun login(context: Context, loginParams: String, loginStateListener: ILoginListener)
    fun logout(loginStateListener: ILoginListener)
    fun checkAndLaunchDeeplinkActivity(intent: Intent, context: Context)

    fun openDeeplink(link:String,context: Context)

    fun getUserConsent():String?{
        return null
    }
    fun initiatePayment(
        paymentInfo: String,
        callback: (intent: Intent) -> Unit
    )
    fun getLifeCycleCallbacks(
        event: Lifecycle.Event
    ){
    }
    fun addCookieList(onCookieAdded:(List<Cookie>) -> Unit)
    fun addHeaders(onHeadersAdded:(HashMap<String, String>) -> Unit)

    fun fireEvent(context: Context, url: String, event: JSONObject)
    fun isInternetConnected(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isConnectedNewApi(context)
        } else {
            isConnectedOld(context)
        }
    }

    fun showError(context: Context, error: String){
        if (!(context as Activity).isFinishing) {
            val alertDialog = AlertDialog.Builder(context).create()
            alertDialog.setTitle(context.getString(R.string.error))
            alertDialog.setMessage(error)
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL, context.getString(R.string.close)
            ) { _, _ -> context.finish() }
            alertDialog.setCancelable(false)
            if (!context.isFinishing) {
                alertDialog.show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
        }
    }
    fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo =
                context.packageManager.getPackageInfoCompat(packageName = context.applicationContext.packageName)
            val data = HashMap<String, String>()
            data[V_CODE] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                packageInfo.versionCode.toString()
            }
            data[V_NAME] = packageInfo.versionName
            data[ANDROID_VERSION] = currentAndroidVersion()
            data[MANUFACTURER] = Build.MANUFACTURER
            data[MODEL] = Build.MODEL

            JSONObject(data as Map<*, *>).encode()
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException("Could not get package name: $e")
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocation(context: Context, isPermissionNeeded: Boolean = true, permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>, callback: (location: Location?)-> Unit) {
        if(isLocationPermissionGranted(context = context)){
            if (isLocationEnabled(context)){
                fetchFusedLocation(context, callback)
            } else if(isPermissionNeeded){
                showSettingsAlert(context)
            }
        } else if (isPermissionNeeded) {
            requestLocationPermission(context, permissionLauncher)
        } else {
            callback(null)
        }
    }

    fun navigateToSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        with(intent) {
            data = Uri.fromParts("package", context.packageName, null)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        context.startActivity(intent)
    }

    fun downloadPdf(context: Context, url: String?, fileName: String?): Long {
        val extension = url?.substring(url.lastIndexOf("."))
        val downloadReference: Long
        val dm: DownloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            fileName + extension
        )
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setTitle(fileName)
        Toast.makeText(context, "Starting Download..", Toast.LENGTH_SHORT).show()
        downloadReference = dm.enqueue(request)
        return downloadReference
    }

    fun shareMessage(
        context: Context,
        shareMessage: String,
        imageUrl: String,
        permissionRequestLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    ) {
        try {
            if (imageUrl.isNotEmpty() && isStoragePermissionGranted(context,permissionRequestLauncher)){
                getBitmapFromUrl(context = context, imageUrl = imageUrl){ bitmapUri : Uri? ->
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "image/*"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                    bitmapUri?.let { shareIntent.putExtra(Intent.EXTRA_STREAM, it) }
                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                }
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                context.startActivity(Intent.createChooser(shareIntent, "Share"))
            }
        } catch (_: Exception) {
        }
    }

    fun copyToClipboard(context: Context, textToCopy: String) {
        val clipboard: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", textToCopy)
        clipboard.setPrimaryClip(clip)
    }

    fun openCamera(
        activity: Activity,
        cameraLauncher: ActivityResultLauncher<Intent>,
        imageUri: Uri,
        permissionRequestLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
    ){
        if(isCameraPermissionGranted(
                context = activity,
                permissionRequestLauncher = permissionRequestLauncher
            )){
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(
                MediaStore.EXTRA_OUTPUT,
                imageUri
            )

            cameraLauncher.launch(takePictureIntent)
        }
    }

    fun openGallery(activity: Activity,galleryLauncher: ActivityResultLauncher<Intent>, imageUri: Uri) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_STREAM, imageUri)
        galleryLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    fun getShortLivedTokenForTripMoney(request:String,shortLivedTokenCallback: ShortLivedTokenCallback)

    fun getLoginStatus(): Boolean

    fun getTripMoneyToken(): String

    fun saveTripMoneyToken(tripMoneyToken: String)

    fun getUserProfileType() : String {
        return Constants.DEFAULT_PROFILE_TYPE
    }


    companion object {
        private const val V_CODE = "app_version_code"
        private const val V_NAME = "app_version_name"
        private const val ANDROID_VERSION = "os_version"
        private const val MANUFACTURER = "manufacturer"
        private const val MODEL = "model"
    }
}