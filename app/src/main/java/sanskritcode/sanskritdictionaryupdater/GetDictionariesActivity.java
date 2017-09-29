package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.io.Files;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// See comment in MainActivity.java for a rough overall understanding of the code.
public class GetDictionariesActivity extends Activity {
    private SharedPreferences.Editor dictVersionEditor;
    private final ArrayList<String> dictionariesSelectedLst = new ArrayList<>();
    private final List<String> dictFiles = new ArrayList<>();
    private List<Boolean> dictFailure = new ArrayList<>();

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
        //noinspection unchecked
        DictIndexStore dictIndexStore = (DictIndexStore) getIntent().getSerializableExtra("dictIndexStore");
        dictionariesSelectedLst.addAll(dictIndexStore.dictionariesSelectedSet);
        dictFailure = new ArrayList<>(Collections.nCopies(dictionariesSelectedLst.size(), false));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(getLocalClassName(), "Got write permissions");
        } else {
            Log.e(getLocalClassName(), "Don't have write permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            Log.i(getLocalClassName(), "new permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE));
        }

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

        DictDownloader dictDownloader = new DictDownloader(this, button, dictFailure, dictFiles, dictionariesSelectedLst, downloadsDir, progressBar, topText);
        dictDownloader.getDictionaries(0);
    }

    public void buttonPressed1(@SuppressWarnings("UnusedParameters") View v) {
        finish();
    }

    private void whenAllDictsExtracted() {
        topText.setText(getString(R.string.finalMessage));
        List<String> dictNames = Lists.transform(dictionariesSelectedLst, new Function<String, String>() {
            public String apply(String in) {
                return Files.getNameWithoutExtension(in);
            }
        });
        final StringBuilder failures = new StringBuilder("");
        for (int i = 0; i < dictNames.size(); i++) {
            //noinspection StatementWithEmptyBody
            if (dictFailure.get(i)) {
                failures.append("\n").append(dictNames.get(i));
            } else {
            }
        }
        if (failures.length() > 0) topText.append("\n" + "Failed on:" + failures);
        StringBuilder successes = new StringBuilder("");
        for (int i = 0; i < dictNames.size(); i++) {
            //noinspection StatementWithEmptyBody
            if (dictFailure.get(i)) {
            } else {
                successes.append("\n").append(dictNames.get(i));
            }
        }
        if (successes.length() > 0) topText.append("\n" + "Succeeded on:" + successes);

        button.setText(R.string.buttonValQuit);
        if (failures.length() == 0) {
            button.setEnabled(true);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishAffinity();
                }
            });
        } else {
            button.setEnabled(false);
        }

        final Activity thisActivity = this;
        button_2.setText(R.string.PROBLEM_SEND_LOG);
        button_2.setVisibility(View.VISIBLE);
        button_2.setEnabled(true);
        button_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ErrorHandler.sendLoagcatMail(thisActivity);
                finishAffinity();
            }
        });

    }

    void extractDicts(int index) {
        if (index >= dictFiles.size()) {
            whenAllDictsExtracted();
        } else {
            new DictExtractor(this, dictDir, dictFailure,
                    dictFiles, downloadsDir,
                    progressBar, topText)
                    .execute(index);
        }
    }


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
            Log.w("DictExtractor", "Storing default dictionary version for " + fileName);
            dictVersionEditor.putString(dictName, getString(R.string.defaultDictVersion));
            dictVersionEditor.commit();
        }
    }

    @Override
    public void onBackPressed() {
        // Don't navigate away in the midst of downloading dictionaries!
        if (button_2.getVisibility() == View.VISIBLE) {
            finish();
        }
    }

}
