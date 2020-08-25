package sanskritCode.downloaderFlow

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.*
import java.util.*

// See comment in MainActivity.java for a rough overall understanding of the code.
// This activity gets archive urls and displays them for the user to choose.
class GetUrlActivity : BaseActivity() {
    internal val LOGGER_TAG = javaClass.getSimpleName()

    private var layout: LinearLayout? = null
    internal var topText: TextView? = null
    private var button: Button? = null
    private val archiveCheckBoxes = HashMap<String, CheckBox>()
    private val indexCheckBoxes = HashMap<String, CheckBox>()

    private val archiveCheckboxListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        val archiveInfo = ArchiveInfo(buttonView.hint.toString())
        if (isChecked) {
            archiveIndexStore!!.archivesSelectedMap[archiveInfo.url] = archiveInfo
        } else {
            archiveIndexStore!!.archivesSelectedMap.remove(archiveInfo.url)
        }
        enableButtonIfArchivesSelected()
    }

    private val indexCheckboxListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        for (archiveInfo in archiveIndexStore!!.indexedArchives[buttonView.hint.toString()]!!) {
            archiveCheckBoxes[archiveInfo.url]?.isChecked = isChecked
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************")
        layout = findViewById(R.id.get_url_layout)
        if (archiveIndexStore == null) {
            archiveIndexStore = intent.getSerializableExtra("archiveIndexStore") as ArchiveIndexStore
        }
        Log.d(LOGGER_TAG, ":onCreate:" + "whenActivityLoaded archiveIndexStore.indexedArchives " + archiveIndexStore!!.indexedArchives)
        Log.d(LOGGER_TAG, ":onCreate:" + "indexesSelected " + archiveIndexStore!!.indexesSelected.toString())
        setContentView(R.layout.activity_get_url)
        topText = findViewById(R.id.df_get_url_textView)
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText?.movementMethod = LinkMovementMethod.getInstance()

        button = findViewById(R.id.df_get_url_button)
        button!!.text = getString(R.string.buttonWorking)
        button!!.isEnabled = false

        setContentView(R.layout.activity_get_url)
        archiveIndexStore!!.getIndexedArchivesSetCheckboxes(this)
    }

    // Called when another activity comes inbetween and is dismissed.
    override fun onResume() {
        super.onResume()
        Log.i(LOGGER_TAG, "onResume:" + "************************STARTS****************************")
        this.showNetworkInfo(findViewById<View>(R.id.df_get_url_textView2) as TextView)
    }


    private fun enableButtonIfArchivesSelected() {
        button = findViewById(R.id.df_get_url_button)
        button!!.isEnabled = !archiveIndexStore!!.archivesSelectedMap.isEmpty()
        val estimatedSize = archiveIndexStore!!.estimateArchivesSelectedMBs()
        val message = String.format(getString(R.string.df_get_archives_button), archiveIndexStore!!.archivesSelectedMap.size, estimatedSize)
        button!!.setText(message)
        Log.d(LOGGER_TAG, ":enableButtonIfArchivesSelected:" + "button enablement " + button!!.isEnabled)
    }

    @Suppress("UNUSED_PARAMETER")
    fun buttonPressed1(v: View) {
        val intent = Intent(this, GetArchivesActivity::class.java)
        intent.putExtra("archiveIndexStore", archiveIndexStore)
        startActivity(intent)
    }

    // TODO: In certain cases, it could be that the destination directory is cleared (eg. by some antivirus). In this case, all dictionaries should be selected.
    private fun selectCheckboxes(archivesSelectedSet: Set<String>?) {
        val sharedArchiveVersionStore = getSharedPreferences(
                getString(R.string.df_archive_version_store), Context.MODE_PRIVATE)
        val sharedArchiveInfoStore = getSharedPreferences(
                getString(R.string.df_archive_info_store), Context.MODE_PRIVATE)
        val sharedArchiveInfoStoreEditor = sharedArchiveInfoStore.edit()
        val sharedArchiveVersionStoreEditor = sharedArchiveVersionStore.edit()
        if (archivesSelectedSet == null) {
            for (cb in archiveCheckBoxes.values) {
                // handle values: kRdanta-rUpa-mAlA -> 2016-02-20_23-22-27
                if (cb.text.endsWith("indexGettingFailed")) {
                    cb.isChecked = false
                    cb.isEnabled = false
                    continue
                }
                val archiveInfo = ArchiveInfo(cb.hint.toString())
                val filename = archiveInfo.url
                var proposedVersionNewer = true

                if (sharedArchiveInfoStore.contains(archiveInfo.url)) {
                    val archiveInfoOlder = ArchiveInfo(sharedArchiveInfoStore.getString(archiveInfo.url, null)!!)
                    proposedVersionNewer = archiveInfo.isVersionNewerThan(archiveInfoOlder)
                } else{
                    val archiveName = ArchiveNameHelper.getArchiveNameAndVersion(filename)[0]
                    // Handle transition from old style records.
                    if (sharedArchiveVersionStore.contains(archiveName)){
                        val currentVersion = sharedArchiveVersionStore.getString(archiveName, getString(R.string.df_defaultArchiveVersion))!!
                        val archiveInfoOlder = ArchiveInfo.fromUrl(archiveInfo.url, currentVersion)
                        archiveInfoOlder.setVersion(currentVersion)
                        archiveInfoOlder.storeToSharedPreferences(sharedArchiveInfoStoreEditor)
                        sharedArchiveVersionStoreEditor.remove(archiveName)
                        sharedArchiveVersionStoreEditor.apply()
                        proposedVersionNewer = archiveInfo.isVersionNewerThan(archiveInfoOlder)
                    }
                }

                if (proposedVersionNewer) {
                    cb.isChecked = true
                } else {
                    cb.isChecked = false
                    archiveIndexStore!!.autoUnselectedArchives++
                }
            }
        } else {
            for (cb in archiveCheckBoxes.values) {
                val archiveInfo = ArchiveInfo(cb.hint.toString())
                val filename = archiveInfo.url
                if (archivesSelectedSet.contains(filename)) {
                    cb.isChecked = true
                } else {
                    cb.isChecked = false
                }
            }
        }

        // checkbox-change listener is only called if there is a change - not if all checkboxes are unselected to start off.
        enableButtonIfArchivesSelected()

        val message = String.format(getString(R.string.df_autoSelectedArchivesMessage), sharedArchiveVersionStore!!.all.size, archiveIndexStore!!.autoUnselectedArchives)
        topText?.append(message)
        Log.d(LOGGER_TAG, ":selectCheckboxes:$message")
    }

    internal fun addCheckboxes(indexedArchives: Map<String, List<ArchiveInfo>>, archivesSelectedSet: Set<String>?) {
        layout = findViewById(R.id.get_url_layout)
        for (indexName in indexedArchives.keys) {
            val indexBox = CheckBox(applicationContext)
            indexBox.setBackgroundColor(Color.YELLOW)
            indexBox.setTextColor(Color.BLACK)
            indexBox.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            indexBox.visibility = View.VISIBLE
            indexBox.setText(String.format(getString(R.string.df_fromSomeIndex), indexName))
            indexBox.hint = indexName
            indexBox.setOnCheckedChangeListener(indexCheckboxListener)
            indexCheckBoxes[indexName] = indexBox
            layout!!.addView(indexBox)

            for (archiveInfo in indexedArchives[indexName]!!) {
                val cb = CheckBox(applicationContext)
                val url = archiveInfo.url
                if (url.endsWith("indexGettingFailed")) {
                    cb.setText(url)
                    cb.setBackgroundColor(Color.RED)
                    archiveCheckBoxes[url] = cb
                    cb.isEnabled = false
                    cb.isChecked = false
                    cb.setTextColor(Color.BLACK)
                    layout!!.addView(cb, layout!!.childCount)
                } else {
                    val cbText = fileNameFromUrl(url=url).replace(".tar.gz", "")
                    cb.setText(cbText)
                    cb.hint = archiveInfo.jsonStr
                    cb.setTextColor(Color.BLACK)
                    layout!!.addView(cb, layout!!.childCount)
                    cb.setOnCheckedChangeListener(archiveCheckboxListener)
                    archiveCheckBoxes[url] = cb
                }
            }
        }

        val message = String.format(getString(R.string.df_added_n_archive_urls), archiveCheckBoxes.size)
        Log.i(LOGGER_TAG, ":addCheckboxes:$message")
        topText = findViewById(R.id.df_get_url_textView)
        topText?.setText(message)
        selectCheckboxes(archivesSelectedSet)
    }


    override fun onBackPressed() {
        super.onBackPressed()
        Log.i(LOGGER_TAG, "Back pressed")
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("archiveIndexStore", archiveIndexStore)
        // intent.putStringArrayListExtra();
        startActivity(intent)
    }

    companion object {
        private val ACTIVITY_NAME = "GetUrlActivity"
    }
}
