package sanskritCode.downloaderFlow

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView

class FinalActivity : BaseActivity() {
    internal val LOGGER_TAG = javaClass.getSimpleName()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************")
        if (archiveIndexStore == null) {
            archiveIndexStore = intent.getSerializableExtra("archiveIndexStore") as ArchiveIndexStore
        }
        setContentView(R.layout.activity_final)

        val topText = findViewById<TextView>(R.id.df_final_act_textView)
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText.movementMethod = LinkMovementMethod.getInstance()
        topText.text = getString(R.string.df_finalMessage)
        largeLog(LOGGER_TAG, ":onCreate:" + archiveIndexStore!!.toString())

        // Suppressed intellij warning about missing commit. storeArchiveVersion() has it.
        val sharedArchiveInfoStore = getSharedPreferences(
                getString(R.string.df_archive_info_store), Context.MODE_PRIVATE)


        val failures = mutableListOf<String>()
        for (archiveInfo in archiveIndexStore!!.archivesSelectedMap.values) {
            if (archiveInfo.status != ArchiveStatus.EXTRACTION_SUCCESS) {
                val archiveShortName = archiveInfo.url.replace(getString(R.string.df_archive_url_redundant_string_regex).toRegex(), "")
                failures.add(ArchiveNameHelper.getNameWithoutAnyExtension(archiveShortName))
            }
        }
        if (failures.size > 0) {
            topText.append(failures.joinToString(prefix = "\n" +
                    "----------------\nFailed on:\n",
                    separator = "\n", postfix = "\n----------------\n"))
            Log.w(LOGGER_TAG, ":onCreate:" + topText.text.toString())
        }
        val successes = mutableListOf<String>()
        val editor = sharedArchiveInfoStore.edit()
        for (archiveInfo in archiveIndexStore!!.archivesSelectedMap.values) {

            if (archiveInfo.status == ArchiveStatus.EXTRACTION_SUCCESS) {
                val archiveShortName = archiveInfo.url.replace(getString(R.string.df_archive_url_redundant_string_regex).toRegex(), "")
                successes.add(ArchiveNameHelper.getNameWithoutAnyExtension(archiveShortName))
                archiveInfo.storeToSharedPreferences(editor)
            }
        }
        if (successes.size > 0) {
            topText.append(successes.joinToString(prefix = "\n" +
                    "----------------\nSucceeded on:\n",
                    separator = "\n", postfix = "\n----------------\n"))
            Log.w(LOGGER_TAG, ":onCreate:" + topText.text.toString())
        }

        val button = findViewById<Button>(R.id.df_final_act_button)
        button.setText(R.string.df_final_act_proceed_btn)
        if (failures.size == 0) {
            button.isEnabled = true
            button.setOnClickListener { finishAffinity() }
        } else {
            button.isEnabled = false
        }

        val thisActivity = this
        val button_2 = findViewById<Button>(R.id.df_final_act_button_2)
        button_2.setText(R.string.PROBLEM_SEND_LOG)
        button_2.visibility = View.VISIBLE
        button_2.isEnabled = true
        button_2.setOnClickListener {
            thisActivity.sendLoagcatMail()
            finishAffinity()
        }
        Log.i(LOGGER_TAG, "onCreate: version: $version")
    }


    override fun onResume() {
        super.onResume()
        Log.i(LOGGER_TAG, "onResume:" + "************************STARTS****************************")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.i(LOGGER_TAG, "Back pressed")
        val intent = Intent(this, GetUrlActivity::class.java)
        intent.putExtra("archiveIndexStore", archiveIndexStore)
        // intent.putStringArrayListExtra();
        startActivity(intent)
    }

    fun buttonPressed1(v: View) {

        val postCompletionActivityName = getString(R.string.df_post_completion_activity)
        if (postCompletionActivityName == "None") {
            finish()
        } else {
            val intent = Intent()
            intent.setComponent(ComponentName(this, postCompletionActivityName))
            intent.putExtra("archiveIndexStore", archiveIndexStore)
            startActivity(intent)
        }
    }
}
