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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.io.Files;

import java.io.File;

public class ExtractDictionariesActivity extends BaseActivity{
    private SharedPreferences.Editor dictVersionEditor;
    final String LOGGER_NAME = getClass().getSimpleName();

    private TextView topText;
    private ProgressBar progressBar;

    private File downloadsDir;
    private File dictDir;
    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOGGER_NAME, "onCreate:" + "************************STARTS****************************");
        SharedPreferences sharedDictVersionStore = getSharedPreferences(
                getString(R.string.dict_version_store), Context.MODE_PRIVATE);
        if (dictIndexStore == null) {
            dictIndexStore = (DictIndexStore) getIntent().getSerializableExtra("dictIndexStore");
        }
        // Suppressed intellij warning about missing commit. storeDictVersion() has it.
        dictVersionEditor = sharedDictVersionStore.edit();
        setContentView(R.layout.activity_extract_dictionaries);
        topText = findViewById(R.id.extract_dict_textView);
        progressBar = findViewById(R.id.extract_dict_progressBar);
        getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, this);

        File sdcard = Environment.getExternalStorageDirectory();
        downloadsDir = new File(sdcard.getAbsolutePath() + "/Download/dicttars");
        if (!downloadsDir.exists()) {
            if (!downloadsDir.mkdirs()) {
                Log.w(LOGGER_NAME, ":onCreate:" + "Returned false while mkdirs " + downloadsDir);
            }
        }
        dictDir = new File(sdcard.getAbsolutePath() + "/dictdata");
        if (!dictDir.exists()) {
            if (!dictDir.mkdirs()) {
                Log.w(LOGGER_NAME, ":onCreate:" + "Returned false while mkdirs " + dictDir);
            }
        }

        new DictExtractor(this, dictDir, dictIndexStore, downloadsDir)
                .execute();
    }

    /* Should only be called from the UI thread! */
    void updateProgressBar(int progress, int total) {
        progressBar.setMax(total);
        progressBar.setProgress(progress);
        progressBar.setVisibility(View.VISIBLE);
    }

    void storeDictVersion(String fileName) {
        String[] filenameParts = DictNameHelper.getDictNameAndVersion(fileName);
        final String dictName = filenameParts[0];
        if (filenameParts.length > 1) {
            String dictVersion = Files.getNameWithoutExtension(Files.getNameWithoutExtension(fileName)).split("__")[1];
            dictVersionEditor.putString(dictName, dictVersion);
            dictVersionEditor.commit();
        } else {
            Log.w(LOGGER_NAME, ":storeDictVersion:" + "Storing default dictionary version for " + fileName);
            dictVersionEditor.putString(dictName, getString(R.string.defaultDictVersion));
            dictVersionEditor.commit();
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, GetUrlActivity.class);
        intent.putExtra("dictIndexStore", dictIndexStore);
        // intent.putStringArrayListExtra();
        startActivity(intent);
    }

    /* Should only be called from the UI thread! */
    void setTopTextWhileExtracting(String archiveName, String contentFileExtracted) {
        String message1 = "Extracting " + archiveName;
        Log.d(LOGGER_NAME,":setTopTextWhileExtracting:" +message1);
        topText.setText(message1);
        topText.append("\n" + getString(R.string.dont_navigate_away));
        topText.append("\n" + "Current file: " + contentFileExtracted);
        this.showNetworkInfo((TextView)findViewById(R.id.extract_dict_textView2));
    }


    /* Should only be called from the UI thread! */
    void whenAllDictsExtracted() {
        Intent intent = new Intent(this, FinalActivity.class);
        intent.putExtra("dictIndexStore", dictIndexStore);
        startActivity(intent);
    }
}
