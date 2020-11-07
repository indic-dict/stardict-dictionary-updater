package sanskritCode.downloaderFlow

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView

import java.io.File

// See comment in MainActivity.java for a rough overall understanding of the code.
class GetArchivesActivity : BaseActivity() {
    internal val LOGGER_TAG = javaClass.getSimpleName()
    private var archiveVersionEditor: SharedPreferences.Editor? = null

    private var topText: TextView? = null
    private var button: Button? = null
    private var button_2: Button? = null
    private var progressBar: ProgressBar? = null


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
        setContentView(R.layout.activity_get_archives)
        topText = findViewById(R.id.df_get_archive_textView)
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText!!.movementMethod = LinkMovementMethod.getInstance()
        button = findViewById(R.id.df_get_archive_button)
        button!!.text = getString(R.string.buttonWorking)
        button!!.isEnabled = false
        button_2 = findViewById(R.id.df_get_archive_button_2)
        button_2!!.visibility = View.INVISIBLE
        button_2!!.isEnabled = false
        progressBar = findViewById(R.id.df_get_archive_progressBar)
        getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, this)

        if (archiveIndexStore!!.archivesSelectedMap.size == 0) {
            topText!!.setText(R.string.no_archives_selected)
            topText!!.append(getString(R.string.df_txtTryAgain))
            button!!.setText(R.string.proceed_button)
            button!!.isEnabled = true
        }

        val archiveDownloader = ArchiveDownloader(this,
                archiveIndexStore!!,
                downloadsDir!!, progressBar!!, topText!!)
        archiveDownloader.downloadArchive(0)
    }

    // Called when another activity comes inbetween and is dismissed.
    override fun onResume() {
        super.onResume()
        Log.i(LOGGER_TAG, "onResume:" + "************************STARTS****************************")
        this.showNetworkInfo(findViewById<View>(R.id.df_get_archive_textView2) as TextView)
    }

    internal fun whenAllDownloaded() {
        val intent = Intent(this, ExtractArchivesActivity::class.java)
        intent.putExtra("archiveIndexStore", archiveIndexStore)
        intent.putExtra("externalDir", externalDir?.uri.toString())
        startActivity(intent)
    }

    /* Should only be called from the UI thread! */
    internal fun updateProgressBar(progress: Int, total: Int) {
        progressBar!!.max = total
        progressBar!!.progress = progress
        progressBar!!.visibility = View.VISIBLE
    }

    override fun onBackPressed() {
        // Don't navigate away in the midst of downloading dictionaries!
        if (button_2!!.visibility == View.VISIBLE) {
            finish()
        }
    }

}
