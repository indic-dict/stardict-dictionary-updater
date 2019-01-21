package sanskritCode.downloaderFlow

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
        val topText = findViewById<TextView>(R.id.final_act_textView)
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText.movementMethod = LinkMovementMethod.getInstance()
        topText.text = getString(R.string.finalMessage)
        largeLog(LOGGER_TAG, ":onCreate:" + archiveIndexStore!!.toString())
        val failures = StringBuilder("")
        for (archiveInfo in archiveIndexStore!!.archivesSelectedMap.values) {

            if (archiveInfo.status != ArchiveStatus.EXTRACTION_SUCCESS) {
                failures.append("\n").append(ArchiveNameHelper.getNameWithoutAnyExtension(archiveInfo.url))
            }
        }
        if (failures.length > 0) {
            topText.append("\nFailed on:$failures")
            Log.w(LOGGER_TAG, ":onCreate:" + topText.text.toString())
        }
        val successes = StringBuilder("")
        for (archiveInfo in archiveIndexStore!!.archivesSelectedMap.values) {

            if (archiveInfo.status == ArchiveStatus.EXTRACTION_SUCCESS) {
                successes.append("\n").append(ArchiveNameHelper.getNameWithoutAnyExtension(archiveInfo.url))
            }
        }
        if (successes.length > 0) {
            topText.append("\nSucceeded on:$successes")
            Log.w(LOGGER_TAG, ":onCreate:" + topText.text.toString())
        }

        val button = findViewById<Button>(R.id.final_act_button)
        button.setText(R.string.buttonValQuit)
        if (failures.length == 0) {
            button.isEnabled = true
            button.setOnClickListener { finishAffinity() }
        } else {
            button.isEnabled = false
        }

        val thisActivity = this
        val button_2 = findViewById<Button>(R.id.final_act_button_2)
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
        finish()
    }
}
