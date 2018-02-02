package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

// See comment in MainActivity.java for a rough overall understanding of the code.
public class GetDictionariesActivity extends BaseActivity {
    private SharedPreferences.Editor dictVersionEditor;

    private TextView topText;
    private Button button;
    private Button button_2;
    private ProgressBar progressBar;

    private File downloadsDir;
    private File dictDir;


    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedDictVersionStore = getSharedPreferences(
                getString(R.string.dict_version_store), Context.MODE_PRIVATE);
        if (dictIndexStore == null) {
            dictIndexStore = (DictIndexStore) getIntent().getSerializableExtra("dictIndexStore");
        }
        // Suppressed intellij warning about missing commit. storeDictVersion() has it.
        dictVersionEditor = sharedDictVersionStore.edit();
        setContentView(R.layout.activity_get_dictionaries);
        topText = findViewById(R.id.get_dict_textView);
        button = findViewById(R.id.get_dict_button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);
        button_2 = findViewById(R.id.get_dict_button_2);
        button_2.setVisibility(View.INVISIBLE);
        button_2.setEnabled(false);
        progressBar = findViewById(R.id.get_dict_progressBar);
        MainActivity.getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, this);

        File sdcard = Environment.getExternalStorageDirectory();
        downloadsDir = new File(sdcard.getAbsolutePath() + "/Download/dicttars");
        if (!downloadsDir.exists()) {
            if (!downloadsDir.mkdirs()) {
                Log.w(getLocalClassName(), "Returned false while mkdirs " + downloadsDir);
            }
        }
        dictDir = new File(sdcard.getAbsolutePath() + "/dictdata");
        if (!dictDir.exists()) {
            if (!dictDir.mkdirs()) {
                Log.w(getLocalClassName(), "Returned false while mkdirs " + dictDir);
            }
        }

        if (dictIndexStore.dictionariesSelectedMap.size() == 0) {
            topText.setText(R.string.no_dicts_selected);
            topText.append(getString(R.string.txtTryAgain));
            button.setText(R.string.proceed_button);
            button.setEnabled(true);
        }

        DictDownloader dictDownloader = new DictDownloader(this,
                dictIndexStore,
                downloadsDir, progressBar, topText);
        dictDownloader.downloadDict(0);
    }

    // Called when another activity comes inbetween and is dismissed.
    @Override
    protected void onResume() {
        super.onResume();
        this.showNetworkInfo((TextView)findViewById(R.id.get_dict_textView2));
    }

    void whenAllDictsDownloaded() {
        Intent intent = new Intent(this, ExtractDictionariesActivity.class);
        intent.putExtra("dictIndexStore", dictIndexStore);
        startActivity(intent);
    }

    /* Should only be called from the UI thread! */
    void updateProgressBar(int progress, int total) {
        progressBar.setMax(total);
        progressBar.setProgress(progress);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        // Don't navigate away in the midst of downloading dictionaries!
        if (button_2.getVisibility() == View.VISIBLE) {
            finish();
        }
    }

}
