package sanskritCode.downloaderFlow

import android.Manifest
import android.content.Intent
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

import java.util.ArrayList

/**
 * Flow: MainActivity::OnCreate ->  IndexGetter -> (user chooses indices, checkboxListener) -> buttonPressed1 ->
 * GetUrlActivity::DictUrlGetter -> (user chooses dictionaries)
 * GetArchivesActivity -> (getDictionaries <-> downloadArchive) -> (extractDict <-> DictExtracter)
 *
 *
 * IntraActivity lifecycle looks like this: http://stackoverflow.com/questions/6509791/onrestart-vs-onresume-android-lifecycle-question
 */
class MainActivity : BaseActivity() {
    private var button: Button? = null
    internal val LOGGER_TAG = javaClass.getSimpleName()

    // Event handler for: When an index is (un) selected.
    private val checkboxListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        if (isChecked) {

            archiveIndexStore!!.indexesSelected[buttonView.text.toString()] = buttonView.hint.toString()
        } else {
            archiveIndexStore!!.indexesSelected.remove(buttonView.text.toString())
        }
        button!!.isEnabled = !archiveIndexStore!!.indexesSelected.isEmpty()
    }

    // Add checkboxes from indexUrls
    internal fun addCheckboxes(indexUrls: Map<String, String>, indexesSelected: Map<String, String>) {
        // retainOnlyOneDictForDebugging();
        val checkBoxes = ArrayList<CheckBox>()
        val layout = findViewById<LinearLayout>(R.id.main_layout)
        for (name in indexUrls.keys) {
            val cb = CheckBox(applicationContext)
            cb.text = name
            cb.hint = indexUrls[name]
            cb.setTextColor(Color.BLACK)
            cb.setOnCheckedChangeListener(checkboxListener)
            layout.addView(cb, layout.childCount)
            checkBoxes.add(cb)
        }
        for (checkBox in checkBoxes) {
            if (indexesSelected.keys.contains(checkBox.text.toString())) {
                checkBox.isChecked = true
            } else {
                checkBox.isChecked = false
            }
        }
        button!!.text = getString(R.string.proceed_button)
        // getDictionaries(0);
    }

    // Called everytime at activity load time, even when back button is pressed - as startActivity(intent) is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (archiveIndexStore == null) {
            archiveIndexStore = ArchiveIndexStore(getString(R.string.index_indexorum))
        }
        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************")
        largeLog(LOGGER_TAG, "onCreate: " + archiveIndexStore!!.toString())
        setContentView(R.layout.activity_main)

        val topText = findViewById<TextView>(R.id.main_textView)
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText.movementMethod = LinkMovementMethod.getInstance()

        button = findViewById(R.id.main_button)
        button!!.text = getString(R.string.buttonWorking)
        button!!.isClickable = false

        getPermission(Manifest.permission.INTERNET, this)
        getPermission(Manifest.permission.ACCESS_NETWORK_STATE, this)
        getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, this)
        archiveIndexStore!!.getIndicesAddCheckboxes(this)
    }

    // Called when another activity comes inbetween and is dismissed.
    override fun onResume() {
        super.onResume()
        Log.i(LOGGER_TAG, "onResume:" + "************************STARTS****************************")
        this.showNetworkInfo(findViewById<View>(R.id.main_textView2) as TextView)
        button!!.setText(R.string.proceed_button)
        button!!.isClickable = true
        //        Log.d(ACTIVITY_NAME, "onResume Indices selected " + archiveIndexStore.indexesSelected.toString());
    }

    // Event handler for: When the proceed button is pressed.
    @Suppress("UNUSED_PARAMETER")
    fun buttonPressed1(v: View) {
        val LOGGER_TAG = "MainActivity:buttonPressed1".substring(0, 26)
        Log.d(LOGGER_TAG, "buttonPressed1: " + "Indices selected " + archiveIndexStore!!.indexesSelected.toString())
        val intent = Intent(this, GetUrlActivity::class.java)
        intent.putExtra("archiveIndexStore", archiveIndexStore)
        // intent.putStringArrayListExtra();
        startActivity(intent)
    }
}
