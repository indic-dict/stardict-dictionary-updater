package sanskritcode.sanskritdictionaryupdater

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView

import com.google.common.base.Function
import com.google.common.collect.Lists
import com.google.common.io.Files

class FinalActivity : BaseActivity() {
    internal val LOGGER_TAG = javaClass.getSimpleName()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************")
        if (dictIndexStore == null) {
            dictIndexStore = intent.getSerializableExtra("dictIndexStore") as DictIndexStore
        }
        setContentView(R.layout.activity_final)
        val topText = findViewById<TextView>(R.id.final_act_textView)
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText.movementMethod = LinkMovementMethod.getInstance()
        topText.text = getString(R.string.finalMessage)
        BaseActivity.largeLog(LOGGER_TAG, ":onCreate:" + dictIndexStore!!.toString())
        val failures = StringBuilder("")
        for (dictInfo in dictIndexStore!!.dictionariesSelectedMap.values) {

            if (dictInfo.status != DictStatus.EXTRACTION_SUCCESS) {
                failures.append("\n").append(DictNameHelper.getNameWithoutAnyExtension(dictInfo.url))
            }
        }
        if (failures.length > 0) {
            topText.append("\nFailed on:$failures")
            Log.w(LOGGER_TAG, ":onCreate:" + topText.text.toString())
        }
        val successes = StringBuilder("")
        for (dictInfo in dictIndexStore!!.dictionariesSelectedMap.values) {

            if (dictInfo.status == DictStatus.EXTRACTION_SUCCESS) {
                successes.append("\n").append(DictNameHelper.getNameWithoutAnyExtension(dictInfo.url))
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
        intent.putExtra("dictIndexStore", dictIndexStore)
        // intent.putStringArrayListExtra();
        startActivity(intent)
    }

    fun buttonPressed1(v: View) {
        finish()
    }
}
