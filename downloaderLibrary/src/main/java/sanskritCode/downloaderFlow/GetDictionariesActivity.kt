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
class GetDictionariesActivity : BaseActivity() {
    internal val LOGGER_TAG = javaClass.getSimpleName()
    private var dictVersionEditor: SharedPreferences.Editor? = null

    private var topText: TextView? = null
    private var button: Button? = null
    private var button_2: Button? = null
    private var progressBar: ProgressBar? = null

    private var downloadsDir: File? = null
    private var dictDir: File? = null


    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************")
        val sharedDictVersionStore = getSharedPreferences(
                getString(R.string.dict_version_store), Context.MODE_PRIVATE)
        if (dictIndexStore == null) {
            dictIndexStore = intent.getSerializableExtra("dictIndexStore") as DictIndexStore
        }
        // Suppressed intellij warning about missing commit. storeDictVersion() has it.
        dictVersionEditor = sharedDictVersionStore.edit()
        setContentView(R.layout.activity_get_dictionaries)
        topText = findViewById(R.id.get_dict_textView)
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText!!.movementMethod = LinkMovementMethod.getInstance()
        button = findViewById(R.id.get_dict_button)
        button!!.text = getString(R.string.buttonWorking)
        button!!.isEnabled = false
        button_2 = findViewById(R.id.get_dict_button_2)
        button_2!!.visibility = View.INVISIBLE
        button_2!!.isEnabled = false
        progressBar = findViewById(R.id.get_dict_progressBar)
        getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, this)

        val sdcard = Environment.getExternalStorageDirectory()
        downloadsDir = File(sdcard.absolutePath + "/Download/dicttars")
        if (!downloadsDir!!.exists()) {
            if (!downloadsDir!!.mkdirs()) {
                Log.w(LOGGER_TAG, ":onCreate:Returned false while mkdirs $downloadsDir")
            }
        }
        dictDir = File(sdcard.absolutePath + "/dictdata")
        if (!dictDir!!.exists()) {
            if (!dictDir!!.mkdirs()) {
                Log.w(LOGGER_TAG, ":onCreate:Returned false while mkdirs $dictDir")
            }
        }

        if (dictIndexStore!!.dictionariesSelectedMap.size == 0) {
            topText!!.setText(R.string.no_dicts_selected)
            topText!!.append(getString(R.string.txtTryAgain))
            button!!.setText(R.string.proceed_button)
            button!!.isEnabled = true
        }

        val dictDownloader = DictDownloader(this,
                dictIndexStore!!,
                downloadsDir!!, progressBar!!, topText!!)
        dictDownloader.downloadDict(0)
    }

    // Called when another activity comes inbetween and is dismissed.
    override fun onResume() {
        super.onResume()
        Log.i(LOGGER_TAG, "onResume:" + "************************STARTS****************************")
        this.showNetworkInfo(findViewById<View>(R.id.get_dict_textView2) as TextView)
    }

    internal fun whenAllDictsDownloaded() {
        val intent = Intent(this, ExtractDictionariesActivity::class.java)
        intent.putExtra("dictIndexStore", dictIndexStore)
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
