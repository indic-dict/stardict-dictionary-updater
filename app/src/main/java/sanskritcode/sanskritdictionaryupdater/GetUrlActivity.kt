package sanskritcode.sanskritdictionaryupdater

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView

import java.util.HashMap

// See comment in MainActivity.java for a rough overall understanding of the code.
class GetUrlActivity : BaseActivity() {
    internal val LOGGER_TAG = javaClass.getSimpleName()

    private var layout: LinearLayout? = null
    internal var topText: TextView? = null
    private var button: Button? = null
    private val dictCheckBoxes = HashMap<String, CheckBox>()
    private val indexCheckBoxes = HashMap<String, CheckBox>()

    private var sharedDictVersionStore: SharedPreferences? = null

    private val dictCheckboxListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        if (isChecked) {
            dictIndexStore!!.dictionariesSelectedMap[buttonView.hint.toString()] = DictInfo(buttonView.hint.toString())
        } else {
            dictIndexStore!!.dictionariesSelectedMap.remove(buttonView.hint.toString())
        }
        enableButtonIfDictsSelected()
    }

    private val indexCheckboxListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        for (url in dictIndexStore!!.indexedDicts[buttonView.hint.toString()]!!) {
            dictCheckBoxes[url]!!.isChecked = isChecked
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************")
        layout = findViewById(R.id.get_url_layout)
        sharedDictVersionStore = getSharedPreferences(
                getString(R.string.dict_version_store), Context.MODE_PRIVATE)
        if (dictIndexStore == null) {
            dictIndexStore = intent.getSerializableExtra("dictIndexStore") as DictIndexStore
        }
        Log.d(LOGGER_TAG, ":onCreate:" + "whenActivityLoaded dictIndexStore.indexedDicts " + dictIndexStore!!.indexedDicts)
        Log.d(LOGGER_TAG, ":onCreate:" + "indexesSelected " + dictIndexStore!!.indexesSelected.toString())
        setContentView(R.layout.activity_get_url)
        topText = findViewById(R.id.get_url_textView)
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText?.movementMethod = LinkMovementMethod.getInstance()

        button = findViewById(R.id.get_url_button)
        button!!.text = getString(R.string.buttonWorking)
        button!!.isEnabled = false

        setContentView(R.layout.activity_get_url)
        dictIndexStore!!.getIndexedDictsSetCheckboxes(this)
    }

    // Called when another activity comes inbetween and is dismissed.
    override fun onResume() {
        super.onResume()
        Log.i(LOGGER_TAG, "onResume:" + "************************STARTS****************************")
        this.showNetworkInfo(findViewById<View>(R.id.get_url_textView2) as TextView)
    }


    private fun enableButtonIfDictsSelected() {
        button = findViewById(R.id.get_url_button)
        button!!.isEnabled = !dictIndexStore!!.dictionariesSelectedMap.isEmpty()
        val estimatedSize = dictIndexStore!!.estimateDictionariesSelectedMBs()
        val message = String.format(getString(R.string.get_dicts_button), dictIndexStore!!.dictionariesSelectedMap.size, estimatedSize)
        button!!.setText(message)
        Log.d(LOGGER_TAG, ":enableButtonIfDictsSelected:" + "button enablement " + button!!.isEnabled)
    }

    @Suppress("UNUSED_PARAMETER")
    fun buttonPressed1(v: View) {
        val intent = Intent(this, GetDictionariesActivity::class.java)
        intent.putExtra("dictIndexStore", dictIndexStore)
        startActivity(intent)
    }

    // TODO: In certain cases, it could be that the dictdata directory is cleared (eg. by some antivirus). In this case, all dictionaries should be selected.
    private fun selectCheckboxes(dictionariesSelectedSet: Set<String>?) {
        if (dictionariesSelectedSet == null) {
            for (cb in dictCheckBoxes.values) {
                // handle values: kRdanta-rUpa-mAlA -> 2016-02-20_23-22-27
                val filename = cb.hint.toString()
                var proposedVersionNewer = true

                val dictnameParts = DictNameHelper.getDictNameAndVersion(filename)
                val dictname = dictnameParts[0]
                if (sharedDictVersionStore!!.contains(dictname)) {
                    val currentVersion = sharedDictVersionStore!!.getString(dictname, getString(R.string.defaultDictVersion))
                    var proposedVersion = getString(R.string.defaultDictVersion)
                    if (dictnameParts.size > 1) {
                        proposedVersion = dictnameParts[1]
                    }

                    proposedVersionNewer = proposedVersion.compareTo(currentVersion!!) > 1
                }

                if (proposedVersionNewer) {
                    cb.isChecked = true
                } else {
                    cb.isChecked = false
                    dictIndexStore!!.autoUnselectedDicts++
                }
            }
        } else {
            for (cb in dictCheckBoxes.values) {
                val filename = cb.hint.toString()
                if (dictionariesSelectedSet.contains(filename)) {
                    cb.isChecked = true
                } else {
                    cb.isChecked = false
                }
            }
        }

        // checkbox-change listener is only called if there is a change - not if all checkboxes are unselected to start off.
        enableButtonIfDictsSelected()

        @SuppressLint("DefaultLocale") val message = String.format("Based on what we remember installing (%1d dicts) from earlier runs of this app (>=  2.9) on this device, we have auto-unselected ~ %2d dictionaries which don\\'t seem to be new or updated. You can reselect.", sharedDictVersionStore!!.all.size, dictIndexStore!!.autoUnselectedDicts)
        topText?.append(message)
        Log.d(LOGGER_TAG, ":selectCheckboxes:$message")
    }

    internal fun addCheckboxes(indexedDicts: Map<String, List<String>>, dictionariesSelectedSet: Set<String>?) {
        layout = findViewById(R.id.get_url_layout)
        for (indexName in indexedDicts.keys) {
            val indexBox = CheckBox(applicationContext)
            indexBox.setBackgroundColor(Color.YELLOW)
            indexBox.setTextColor(Color.BLACK)
            indexBox.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
            indexBox.visibility = View.VISIBLE
            indexBox.setText(String.format(getString(R.string.fromSomeIndex), indexName))
            indexBox.hint = indexName
            indexBox.setOnCheckedChangeListener(indexCheckboxListener)
            indexCheckBoxes[indexName] = indexBox
            layout!!.addView(indexBox)

            for (url in indexedDicts[indexName]!!) {
                val cb = CheckBox(applicationContext)
                cb.setText(url.replace(".*/".toRegex(), ""))
                cb.hint = url
                cb.setTextColor(Color.BLACK)
                layout!!.addView(cb, layout!!.childCount)
                cb.setOnCheckedChangeListener(dictCheckboxListener)
                dictCheckBoxes[url] = cb
            }
        }

        val message = String.format(getString(R.string.added_n_dictionary_urls), dictCheckBoxes.size)
        Log.i(LOGGER_TAG, ":addCheckboxes:$message")
        topText = findViewById(R.id.get_url_textView)
        topText?.setText(message)
        selectCheckboxes(dictionariesSelectedSet)
    }

    companion object {
        private val ACTIVITY_NAME = "GetUrlActivity"
    }
}
