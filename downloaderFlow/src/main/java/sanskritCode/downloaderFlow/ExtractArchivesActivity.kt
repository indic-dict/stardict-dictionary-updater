package sanskritCode.downloaderFlow

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView

import com.google.common.io.Files

import java.io.File

class ExtractArchivesActivity : BaseActivity() {
    private var archiveVersionEditor: SharedPreferences.Editor? = null
    internal val LOGGER_TAG = javaClass.getSimpleName()

    private var topText: TextView? = null
    private var progressBar: ProgressBar? = null

    private var downloadsDir: File? = null
    private var destDir: File? = null
    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************")
        val sharedArchiveVersionStore = getSharedPreferences(
                getString(R.string.df_archive_version_store), Context.MODE_PRIVATE)
        if (archiveIndexStore == null) {
            archiveIndexStore = intent.getSerializableExtra("archiveIndexStore") as ArchiveIndexStore
        }
        // Suppressed intellij warning about missing commit. storeArchiveVersion() has it.
        archiveVersionEditor = sharedArchiveVersionStore.edit()
        setContentView(R.layout.activity_extract_archives)
        topText = findViewById(R.id.df_extract_archive_textView)
        progressBar = findViewById(R.id.df_extract_archive_progressBar)
        getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, this)

        val sdcard = Environment.getExternalStorageDirectory()
        downloadsDir = File(sdcard.absolutePath, getString(R.string.df_downloadsDir))
        if (!downloadsDir!!.exists()) {
            if (!downloadsDir!!.mkdirs()) {
                Log.w(LOGGER_TAG, ":onCreate:Returned false while mkdirs $downloadsDir")
            }
        }
        destDir = File(sdcard.absolutePath, getString(R.string.df_destination_sdcard_directory))
        if (!destDir!!.exists()) {
            if (!destDir!!.mkdirs()) {
                Log.w(LOGGER_TAG, ":onCreate:Returned false while mkdirs $destDir")
            }
        }

        ArchiveExtractor(this, destDir!!, archiveIndexStore!!, downloadsDir!!)
                .execute()
    }

    override fun onResume() {
        super.onResume()
        Log.i(LOGGER_TAG, "onResume:" + "************************STARTS****************************")
    }

    /* Should only be called from the UI thread! */
    internal fun updateProgressBar(progress: Int, total: Int) {
        progressBar!!.max = total
        progressBar!!.progress = progress
        progressBar!!.visibility = View.VISIBLE
    }

    internal fun storeArchiveVersion(fileName: String) {
        val filenameParts = ArchiveNameHelper.getArchiveNameAndVersion(fileName)
        val archiveName = filenameParts[0]
        if (filenameParts.size > 1) {
            val archiveVersion = Files.getNameWithoutExtension(Files.getNameWithoutExtension(fileName)).split("__".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1]
            archiveVersionEditor!!.putString(archiveName, archiveVersion)
            archiveVersionEditor!!.commit()
        } else {
            Log.w(LOGGER_TAG, ":storeArchiveVersion:Storing default archive version for $fileName")
            archiveVersionEditor!!.putString(archiveName, getString(R.string.df_defaultArchiveVersion))
            archiveVersionEditor!!.commit()
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        Log.i(LOGGER_TAG, "Back pressed.")
        val intent = Intent(this, GetUrlActivity::class.java)
        intent.putExtra("archiveIndexStore", archiveIndexStore)
        // intent.putStringArrayListExtra();
        startActivity(intent)
    }

    /* Should only be called from the UI thread! */
    internal fun setTopTextWhileExtracting(archiveName: String, contentFileExtracted: String) {
        val message1 = "Extracting $archiveName"
        Log.d(LOGGER_TAG, ":setTopTextWhileExtracting:$message1")
        topText!!.text = message1
        topText!!.append("\n" + getString(R.string.dont_navigate_away))
        topText!!.append("\nCurrent file: $contentFileExtracted")
        this.showNetworkInfo(findViewById<View>(R.id.df_extract_archive_textView2) as TextView)
    }


    /* Should only be called from the UI thread! */
    internal fun whenAllExtracted() {
        val intent = Intent(this, FinalActivity::class.java)
        intent.putExtra("archiveIndexStore", archiveIndexStore)
        startActivity(intent)
    }
}
