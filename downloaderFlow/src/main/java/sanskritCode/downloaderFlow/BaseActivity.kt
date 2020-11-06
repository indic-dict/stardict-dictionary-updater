package sanskritCode.downloaderFlow

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.DocumentsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import org.apache.commons.io.input.ReversedLinesFileReader
import java.io.File
import java.io.IOException
import java.nio.charset.Charset


fun fileNameFromUrl(url: String): String {
    // return Paths.get(URI(url).getPath()).getFileName().toString() requires min API 26, so not doing that.
    if (url.lastIndexOf("/") > -1) {
        return url.substring(url.lastIndexOf("/") + 1).replace("/", "")
    } else {
        Log.e("ArchiveIndexStore", "$url has no /.")
        return url;
    }
}

abstract class BaseActivity : AppCompatActivity() {
    protected var archiveIndexStore: ArchiveIndexStore? = null
    val GET_EXTERNAL_DIR = 1
    protected var externalDir:File? = null
    protected var downloadsDir: File? = null
    protected var destDir: File? = null


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
        outState.putSerializable("archiveIndexStore", archiveIndexStore)
    }

    protected fun setDirectories() {
        val LOGGER_TAG = localClassName
        if (externalDir == null) {
            Log.e(LOGGER_TAG, "Could not get access to external directory!")
        }
        downloadsDir = File(externalDir?.absolutePath, getString(R.string.df_downloadsDir))
        destDir = File(externalDir?.absolutePath, getString(R.string.df_extraction_directory))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            if(savedInstanceState.containsKey("archiveIndexStore")) {
                archiveIndexStore = savedInstanceState.get("archiveIndexStore") as ArchiveIndexStore
            }
            if(savedInstanceState.containsKey("externalDir")) {
                externalDir = savedInstanceState.get("externalDir") as File
                setDirectories()
            }
        } else {
            if (externalDir == null && intent.hasExtra("externalDir")) {
                externalDir = intent.getSerializableExtra("externalDir") as File
                setDirectories()
            } else {
//                TODO: Remove this branch once directory selection is fixed.
                externalDir = Environment.getExternalStorageDirectory()
                setDirectories()
            }
        }
    }

    fun sendLogcatMail(mailBody: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED) {
            Log.d(this.localClassName, "SendLogcatMail: " + "Got READ_LOGS permissions")
        } else {
            Log.e(this.localClassName, "SendLogcatMail: " + "Don't have READ_LOGS permissions")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_LOGS), 103)
            Log.i(this.localClassName, "SendLogcatMail: " + "new READ_LOGS permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS))
        }


        var deviceDetails = "Device details:"
        deviceDetails += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")"
        deviceDetails += "\n OS API Level: " + android.os.Build.VERSION.RELEASE + "(" + android.os.Build.VERSION.SDK_INT + ")"
        deviceDetails += "\n Device: " + android.os.Build.DEVICE
        deviceDetails += "\n Model (and Product): " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")"
        Log.i(this.localClassName, "SendLogcatMail: deviceDetails: $deviceDetails")


        //send log using email
        val emailIntent = Intent(Intent.ACTION_SEND)
        // Set type to "email"
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        emailIntent.type = "vnd.android.cursor.dir/email"
//        emailIntent.setType("application/octet-stream"); /* or use intent.setType("message/rfc822); */
        val subject = "Downloader App Failure report."
        val to = arrayOf(getString(R.string.issueEmailId))
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to)
        // the attachment
        val logcatUri = getLogcatFileUri()
        emailIntent.putExtra(Intent.EXTRA_STREAM, logcatUri)

//        val truncatedLogs = getLogSummary(uri = logcatUri)
//        // Email body can't be too large!
        emailIntent.putExtra(Intent.EXTRA_TEXT, mailBody.takeLast(10000))
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        this.startActivity(Intent.createChooser(emailIntent, "Email failure report to maker?..."))
    }

    private fun getLogcatFileUri(): Uri {
        // save logcat in file
        val outputFile = File(this.externalCacheDir!!.absolutePath,
                "logcat.txt")
        val pInfo: PackageInfo = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0)
        val versionName = pInfo.versionName
        Log.i(this.localClassName, "SendLogcatMail: App version: $versionName")

        try {
            Runtime.getRuntime().exec(
                    "logcat -f " + outputFile.absolutePath)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            Log.e(this.localClassName, "SendLogcatMail: " + "Alas error! ", e)
        }
        Log.i(this.localClassName, "SendLogcatMail: " + "logcat file is " + outputFile.absolutePath)
        val uri = FileProvider.getUriForFile(this, getApplication().getPackageName(), outputFile)
        return uri
    }

    fun showNetworkInfo(warningText: TextView) {
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm?.activeNetworkInfo
        val isConnected = activeNetwork != null && activeNetwork.isConnected
        // Newer way is to use the following, but that requires min api 16.
        // val networkCapabilities = cm.getNetworkCapabilities(cm?.activeNetwork)
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

        fun getLogSummary(uri: Uri): String {
            val file = File(uri.getPath())
            val n_lines = 3000
            val reversedReader = ReversedLinesFileReader(file, Charset.defaultCharset())
            var result: String = ""
            for (i in 0 until n_lines) {
                val line: String = reversedReader.readLine() ?: break
                result += line
            }
            return result.takeLast(900000)
        }

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
