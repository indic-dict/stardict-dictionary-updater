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

class ExtractDictionariesActivity : BaseActivity() {
    private var dictVersionEditor: SharedPreferences.Editor? = null
    internal val LOGGER_TAG = javaClass.getSimpleName()

    private var topText: TextView? = null
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
        setContentView(R.layout.activity_extract_dictionaries)
        topText = findViewById(R.id.extract_dict_textView)
        progressBar = findViewById(R.id.extract_dict_progressBar)
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

        DictExtractor(this, dictDir!!, dictIndexStore!!, downloadsDir!!)
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

    internal fun storeDictVersion(fileName: String) {
        val filenameParts = DictNameHelper.getDictNameAndVersion(fileName)
        val dictName = filenameParts[0]
        if (filenameParts.size > 1) {
            val dictVersion = Files.getNameWithoutExtension(Files.getNameWithoutExtension(fileName)).split("__".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1]
            dictVersionEditor!!.putString(dictName, dictVersion)
            dictVersionEditor!!.commit()
        } else {
            Log.w(LOGGER_TAG, ":storeDictVersion:Storing default dictionary version for $fileName")
            dictVersionEditor!!.putString(dictName, getString(R.string.defaultDictVersion))
            dictVersionEditor!!.commit()
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        Log.i(LOGGER_TAG, "Back pressed.")
        val intent = Intent(this, GetUrlActivity::class.java)
        intent.putExtra("dictIndexStore", dictIndexStore)
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
        this.showNetworkInfo(findViewById<View>(R.id.extract_dict_textView2) as TextView)
    }


    /* Should only be called from the UI thread! */
    internal fun whenAllDictsExtracted() {
        val intent = Intent(this, FinalActivity::class.java)
        intent.putExtra("dictIndexStore", dictIndexStore)
        startActivity(intent)
    }
}
