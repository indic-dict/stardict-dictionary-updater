package sanskritCode.downloaderFlow

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.TextView

import java.io.File
import java.io.IOException

abstract class BaseActivity : Activity() {
    protected var dictIndexStore: DictIndexStore? = null

    protected val version: String
        get() {
            val LOGGER_TAG = localClassName
            try {
                val pInfo = this.packageManager.getPackageInfo(packageName, 0)
                return pInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.w(LOGGER_TAG, "getVersion:" + "Version getting failed.")
                e.printStackTrace()
                return "Version getting failed."
            }

        }

    fun getPermission(permissionString: String, activity: Activity) {
        val LOGGER_TAG = localClassName
        if (ContextCompat.checkSelfPermission(activity, permissionString) == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOGGER_TAG, "getPermission: Got permission: $permissionString")
        } else {
            Log.w(LOGGER_TAG, "getPermission: Don't have permission: $permissionString")
            ActivityCompat.requestPermissions(activity, arrayOf(permissionString), 101)
            Log.i(LOGGER_TAG, "getPermission: new permission: " + ContextCompat.checkSelfPermission(activity, permissionString))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("dictIndexStore", dictIndexStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null && savedInstanceState.containsKey("dictIndexStore")) {
            dictIndexStore = savedInstanceState.get("dictIndexStore") as DictIndexStore
        }
    }

    fun sendLoagcatMail() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(this.localClassName, "SendLoagcatMail: " + "Got READ_LOGS permissions")
        } else {
            Log.e(this.localClassName, "SendLoagcatMail: " + "Don't have READ_LOGS permissions")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_LOGS), 103)
            Log.i(this.localClassName, "SendLoagcatMail: " + "new READ_LOGS permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS))
        }
        // save logcat in file
        val outputFile = File(Environment.getExternalStorageDirectory().absolutePath,
                "logcat.txt")
        Log.i(this.localClassName, "SendLoagcatMail: " + "logcat file is " + outputFile.absolutePath)


        var deviceDetails = "Device details:"
        deviceDetails += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")"
        deviceDetails += "\n OS API Level: " + android.os.Build.VERSION.RELEASE + "(" + android.os.Build.VERSION.SDK_INT + ")"
        deviceDetails += "\n Device: " + android.os.Build.DEVICE
        deviceDetails += "\n Model (and Product): " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")"
        Log.i(this.localClassName, "SendLoagcatMail: deviceDetails: $deviceDetails")

        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        Log.i(this.localClassName, "SendLoagcatMail: App version: $versionName with id $versionCode")

        try {
            Runtime.getRuntime().exec(
                    "logcat -f " + outputFile.absolutePath)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            Log.e(this.localClassName, "SendLoagcatMail: " + "Alas error! ", e)
        }

        //send file using email
        val emailIntent = Intent(Intent.ACTION_SEND)
        // Set type to "email"
        emailIntent.type = "vnd.android.cursor.dir/email"
        val to = arrayOf("vishvas.vasuki+STARDICTAPP@gmail.com")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to)
        // the attachment
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile))
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Stardict Updater App Failure report.")
        this.startActivity(Intent.createChooser(emailIntent, "Email failure report to maker?..."))
    }

    fun showNetworkInfo(warningText: TextView) {
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        var activeNetwork = cm?.activeNetworkInfo
        val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
        val isWiFi = activeNetwork != null && activeNetwork.type == ConnectivityManager.TYPE_WIFI

        warningText.setBackgroundColor(Color.LTGRAY)
        warningText.setTextColor(Color.RED)
        if (isConnected) {
            if (isWiFi) {
                warningText.visibility = View.INVISIBLE
            } else {
                warningText.setText(R.string.connected_nowifi)
                warningText.visibility = View.VISIBLE
            }
        } else {
            warningText.setText(R.string.noConnection)
            warningText.visibility = View.VISIBLE
        }

    }

    companion object {


        fun largeLog(tag: String, content: String) {
            if (content.length > 4000) {
                Log.v(tag, tag + ":" + content.substring(0, 4000))
                largeLog(tag, content.substring(4000))
            } else {
                Log.v(tag, "$tag:$content")
            }
        }
    }

}
