package com.gommt.tripmoney

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient.FileChooserParams
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

const val FILE_CHOOSER_TITLE = "File Chooser"
const val IMAGE_PATTERN = "image/*"

fun isNullOrWhiteSpace(value: String?): Boolean = (value == null) || value.trim().isEmpty()

fun PackageManager.getPackageInfoCompat(
    packageName: String,
    flags: Int = 0
): PackageInfo =
    if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }

fun isLocationPermissionGranted(context: Context): Boolean {
    return !(ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED)
}

fun requestLocationPermission(
    context: Context,
    permissionRequestLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
){
    permissionRequestLauncher.launch(arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ))
}
fun isLocationEnabled(context: Context): Boolean {
    val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
        LocationManager.NETWORK_PROVIDER
    )
}
@SuppressLint("MissingPermission")
fun fetchFusedLocation(context: Context, callback: (location: Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, object : CancellationToken() {
        override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token
        override fun isCancellationRequested() = false
    })

    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
        callback(location)
    }
}

fun showSettingsAlert(context: Context) {
    val alertDialog: AlertDialog.Builder = AlertDialog.Builder(context)

    alertDialog.setTitle("Error!")
    alertDialog.setMessage("Please enable Location services")
    alertDialog.setPositiveButton(
        context.resources.getString(android.R.string.ok)
    ) { _, _ ->
        openLocationSettings(context)
    }
    alertDialog.show()
}

fun openLocationSettings(context: Context){
    val intent = Intent(
        Settings.ACTION_LOCATION_SOURCE_SETTINGS
    )
    context.startActivity(intent)
}

fun openSettings(context: Context){
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}

fun isCameraPermissionGranted(
    context: Context,
    permissionRequestLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
): Boolean {
    return if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        val cameraPermission = Manifest.permission.CAMERA
        val writeStoragePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val readStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE
        val cameraPermissions = arrayOf(cameraPermission, writeStoragePermission, readStoragePermission)
        permissionRequestLauncher.launch(cameraPermissions)
        false
    } else {
        true
    }
}

fun isExternal(url: String): Boolean {
    return url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:") || url.startsWith(
        "smsto:"
    ) || url.startsWith("mms:") || url.startsWith("mmsto:") || url.contains("open=outside") || url.contains(
        "go.ibi.bo"
    )
}

fun isStoragePermissionGranted(
    context: Context,
    permissionRequestLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
): Boolean {
    return if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        permissionRequestLauncher.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        )
        false
    } else {
        true
    }
}

fun getPhotoFileUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "IMG_${timeStamp}.jpeg"
    val dir = context.externalCacheDir ?: context.cacheDir
    val photoFile = File(dir, "/tripMoneyWebView/$fileName")
    if (photoFile.parentFile?.exists() == false) photoFile.parentFile?.mkdir()
    return FileProvider.getUriForFile(
        context,
        context.applicationContext.packageName + ".provider",
        photoFile
    )
}

fun getBitmapFromUrl(context: Context, imageUrl: String, imageCallback: (bitmapUri: Uri?)-> Unit){
    Glide.with(context).asBitmap().load(imageUrl).into(object : CustomTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            imageCallback(resource.getLocalUri(context))
        }

        override fun onLoadCleared(placeholder: Drawable?) {
        }
    })
}

fun Bitmap.getLocalUri(context: Context): Uri? {
    var photoURI: Uri? = null
    try {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "share_image_" + System.currentTimeMillis() + ".png"
        )
        file.delete()
        file.parentFile?.mkdirs()
        file.createNewFile()
        val fileOutputStream = FileOutputStream(file)
        this.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()
        photoURI = FileProvider.getUriForFile(
            context, context.applicationContext.packageName + ".provider", file
        )
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return photoURI
}

@Suppress("DEPRECATION")
fun isConnectedOld(context: Context): Boolean {
    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = connManager.activeNetworkInfo
    return networkInfo?.isConnected == true

}

@RequiresApi(Build.VERSION_CODES.M)
fun isConnectedNewApi(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

fun currentAndroidVersion(): String = java.lang.String(Build.VERSION.RELEASE).replaceAll("(\\d+[.]\\d+)(.*)", "$1")

fun showSettingsSnackBar(context: Context) {
    val content: View = (context as Activity).findViewById(android.R.id.content) ?: return
    val snackBar: Snackbar = Snackbar.make(
        content,
        context.getString(R.string.trip_money_permission_messgae),
        Snackbar.LENGTH_LONG
    ).setAction(context.getString(R.string.go_to_settings)) {
        openSettings(context)
    }
    snackBar.show()
}


private fun retrieveMimeParams(fileChooserParams: FileChooserParams?): Array<String>? {
    if (fileChooserParams != null) {
        val mimeParams: Array<String> = fileChooserParams.acceptTypes
        if (mimeParams.isNotEmpty() && mimeParams[0].trim().isNotEmpty()) {
            return mimeParams
        }
    }
    return null
}

fun openImageUploadChooser(
    isPermissionGranted: Boolean,
    cameraUri: Uri,
    galleryLauncher: ActivityResultLauncher<Intent>,
    fileChooserParams: FileChooserParams?
    ) {
    try {
        val imageIntent = Intent(Intent.ACTION_GET_CONTENT)
        imageIntent.addCategory(Intent.CATEGORY_OPENABLE)
        imageIntent.type = "*/*"
        val mimeParams: Array<String>? = retrieveMimeParams(fileChooserParams)
        if (mimeParams != null) {
            imageIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeParams)
        }
        val chooserIntent = Intent.createChooser(imageIntent, FILE_CHOOSER_TITLE)
        val mediaList: Array<Parcelable?>? = getMediaIntents(isPermissionGranted,fileChooserParams, cameraUri)
        if (mediaList != null) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, mediaList)
        }
        galleryLauncher.launch(chooserIntent)
    } catch (_: Exception) {
    }
}

private fun getMediaIntents(
    includeCamera: Boolean,
    fileChooserParams: FileChooserParams?,
    cameraUri: Uri,
): Array<Parcelable?>? {
    val mediaIntentList: ArrayList<Parcelable> = ArrayList()
    var intentArray: Array<Parcelable?>? = null
    if (includeCamera) {
        addMediaIntentData(mediaIntentList,fileChooserParams, cameraUri)
    }
    if (mediaIntentList.size > 0) {
        intentArray = arrayOfNulls(mediaIntentList.size)
        mediaIntentList.toArray(intentArray)
    }
    return intentArray
}

private fun addMediaIntentData(
    mediaList: MutableList<Parcelable>,
    fileChooserParams: FileChooserParams?,
    cameraUri: Uri,
) {
    var mimeList: List<String>? = null
    val mimeParams = retrieveMimeParams(fileChooserParams)
    if (mimeParams != null) {
        mimeList = listOf(*mimeParams)
    }
    if (mimeList == null || mimeList.contains(IMAGE_PATTERN) || mimeList.contains("*/*")) {
        val mediaIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        mediaIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
        mediaList.add(mediaIntent)
    }
}


fun JSONObject.encode(): String {
    try {
        return URLEncoder.encode(this.toString(), Charsets.UTF_8.name()).replace("+", "%20")
    } catch (_: Exception) {
    }
    return ""
}

var mDeviceId = ""

fun getDeviceId(context: Context): String {
    try {
        if (mDeviceId.isEmpty()) {
            mDeviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    } catch (ex: java.lang.Exception) {
        return ""
    }
    return mDeviceId
}
