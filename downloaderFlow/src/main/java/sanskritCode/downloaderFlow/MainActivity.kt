package sanskritCode.downloaderFlow

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

import java.util.ArrayList

/**
 * Flow: MainActivity::OnCreate ->  IndexGetter -> (user chooses indices, indexCheckboxListener) -> buttonPressed1 ->
 * GetUrlActivity::DictUrlGetter -> (user chooses dictionaries)
 * GetArchivesActivity -> (getDictionaries <-> downloadArchive) -> (extractDict <-> DictExtracter)
 *
 *
 * IntraActivity lifecycle looks like this: http://stackoverflow.com/questions/6509791/onrestart-vs-onresume-android-lifecycle-question
 */
class MainActivity : BaseActivity() {
    private var button: Button? = null
    internal val LOGGER_TAG = javaClass.getSimpleName()
    private var indexCheckboxes = ArrayList<CheckBox>()


    fun openDirectory(pickerInitialUri: Uri) {
        // Choose a directory using the system's file picker.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            // Provide read access to files and sub-directories in the user-selected
            // directory.
            flags = Intent
                    .FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker when it loads.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

        startActivityForResult(intent, GET_EXTERNAL_DIR)
    }


    override fun onActivityResult(
            requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == GET_EXTERNAL_DIR
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                // Perform operations on the document using its URI.
                externalDir = File(uri.path)
                Log.i(LOGGER_TAG, String.format("externalDir %s \n downloadsDir %s \n" +
                        "destDir %s", externalDir?.absolutePath, downloadsDir?.absolutePath, destDir?.absolutePath))
                setDirectories()
                val contentResolver = applicationContext.contentResolver

                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                // Check for the freshest data.
                contentResolver.takePersistableUriPermission(uri, takeFlags)


                val intent = Intent(this, GetUrlActivity::class.java)
                intent.putExtra("archiveIndexStore", archiveIndexStore)
                intent.putExtra("externalDir", externalDir)
                // intent.putStringArrayListExtra();
                startActivity(intent)

            }
        }
    }

    // Event handler for: When an index is (un) selected.
    private val indexCheckboxListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        if (isChecked) {
            archiveIndexStore!!.indexesSelected[buttonView.text.toString()] = buttonView.hint.toString()
        } else {
            archiveIndexStore!!.indexesSelected.remove(buttonView.text.toString())
        }
        button!!.isEnabled = !archiveIndexStore!!.indexesSelected.isEmpty()
    }

    val toggleAllCheckboxListener = android.widget.CompoundButton.OnCheckedChangeListener{
        _, isChecked ->
        if (isChecked) {
            for (cb in indexCheckboxes) {
                cb.isChecked = true
            }
        } else {
            for (cb in indexCheckboxes) {
                cb.isChecked = false
            }
        }
    }

    // Add checkboxes from indexUrls
    internal fun addCheckboxes(indexUrls: Map<String, String>, indexesSelected: Map<String, String>) {
        // retainOnlyOneDictForDebugging();
        indexCheckboxes = ArrayList<CheckBox>()
        val layout = findViewById<LinearLayout>(R.id.df_main_layout)
        for (name in indexUrls.keys) {
            val cb = CheckBox(applicationContext)
            cb.text = name
            cb.hint = indexUrls[name]
            cb.setTextColor(Color.BLACK)
            cb.setOnCheckedChangeListener(indexCheckboxListener)
            layout.addView(cb, layout.childCount)
            indexCheckboxes.add(cb)
        }
        for (checkBox in indexCheckboxes) {
            if (indexesSelected.keys.contains(checkBox.text.toString())) {
                checkBox.isChecked = true
            } else {
                checkBox.isChecked = false
            }
        }
        button!!.text = getString(R.string.set_destination_folder)

        if (indexUrls.size > 0) {
            val toggleAllCheckBox = findViewById<android.widget.CheckBox>(R.id.df_main_toggle_selection_checkbox)
            toggleAllCheckBox.setVisibility(android.view.View.VISIBLE)
            toggleAllCheckBox.isChecked = true
            toggleAllCheckBox.setBackgroundColor(Color.YELLOW)
            toggleAllCheckBox.setOnCheckedChangeListener(toggleAllCheckboxListener)
        }
        // getDictionaries(0);
    }

    // Called everytime at activity load time, even when back button is pressed - as startActivity(intent) is called.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (archiveIndexStore == null) {
            archiveIndexStore = ArchiveIndexStore(getString(R.string.df_index_indexorum))
        }
        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************")
        largeLog(LOGGER_TAG, "onCreate: " + archiveIndexStore!!.toString())
        setContentView(R.layout.activity_main)

        val topText = findViewById<TextView>(R.id.df_main_textView)
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText.movementMethod = LinkMovementMethod.getInstance()

        button = findViewById(R.id.df_main_button)
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
        this.showNetworkInfo(findViewById<View>(R.id.df_main_textView2) as TextView)
        button!!.setText(R.string.proceed_button)
        button!!.isClickable = true
        //        Log.d(ACTIVITY_NAME, "onResume Indices selected " + archiveIndexStore.indexesSelected.toString());
    }

    // Event handler for: When the proceed button is pressed.
    @Suppress("UNUSED_PARAMETER")
    fun buttonPressed1(v: View) {
        val LOGGER_TAG = "MainActivity:buttonPressed1".substring(0, 26)
        Log.d(LOGGER_TAG, "buttonPressed1: " + "Indices selected " + archiveIndexStore!!.indexesSelected.toString())
//        openDirectory(Uri.EMPTY)
//                TODO: Remove this branch once directory selection is fixed.

        val intent = Intent(this, GetUrlActivity::class.java)
        intent.putExtra("archiveIndexStore", archiveIndexStore)
        // intent.putStringArrayListExtra();
        startActivity(intent)
    }
}
