package sanskritCode.downloaderFlow

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView

class ExtractArchivesActivity : BaseActivity() {
    internal val LOGGER_TAG = javaClass.getSimpleName()

    private var topText: TextView? = null
    private var progressBar: ProgressBar? = null

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************")
        setContentView(R.layout.activity_extract_archives)
        topText = findViewById(R.id.df_extract_archive_textView)
        progressBar = findViewById(R.id.df_extract_archive_progressBar)

        if (!downloadsDir!!.exists()) {
            if (!downloadsDir!!.mkdirs()) {
                Log.w(LOGGER_TAG, ":onCreate:Returned false while mkdirs $downloadsDir")
            }
        }
        if (!destDir!!.exists()) {
            Log.w(LOGGER_TAG, ":onCreate: Strange - $destDir does not exist.")
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

    // TODO: Not being invoked.
    fun backPressHandler() {
        // Don't navigate away in the midst of extracting dictionaries!
        // archiveIndexStore.indexedArchives etc.. may need to be reset or handled carefully.
//        super.onBackPressed()
//        Log.i(LOGGER_TAG, "Back pressed.")
//        val intent = Intent(this, GetUrlActivity::class.java)
//        intent.putExtra("archiveIndexStore", archiveIndexStore)
//        intent.putExtra("externalDir", externalDir?.uri.toString())
//        // intent.putStringArrayListExtra();
//        startActivity(intent)
    }

    /* Should only be called from the UI thread! */
    internal fun setTopTextWhileExtracting(archiveName: String, contentFileExtracted: String) {
        val message1 = "Extracting $archiveName"
        topText!!.text = message1
        topText!!.append("\n" + getString(R.string.dont_navigate_away))
        topText!!.append("\nCurrent file: $contentFileExtracted")
        Log.d(LOGGER_TAG, ":setTopTextWhileExtracting:$archiveName - $contentFileExtracted")
        this.showNetworkInfo(findViewById<View>(R.id.df_extract_archive_textView2) as TextView)
    }


    /* Should only be called from the UI thread! */
    internal fun whenAllExtracted() {
        val intent = Intent()
        intent.setComponent(ComponentName(this, getString(R.string.df_post_extraction_activity)))
        intent.putExtra("archiveIndexStore", archiveIndexStore)
        intent.putExtra("externalDir", destDir?.uri.toString())
        startActivity(intent)
    }
}
