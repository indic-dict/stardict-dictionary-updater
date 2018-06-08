package sanskritcode.sanskritdictionaryupdater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// See comment in MainActivity.java for a rough overall understanding of the code.
public class GetUrlActivity extends BaseActivity {
    private static final String ACTIVITY_NAME = "GetUrlActivity";
    final String LOGGER_TAG = getClass().getSimpleName();

    private LinearLayout layout;
    TextView topText;
    private Button button;
    private final Map<String, CheckBox> dictCheckBoxes = new HashMap<>();
    private final Map<String, CheckBox> indexCheckBoxes = new HashMap<>();

    private SharedPreferences sharedDictVersionStore;

    private final CompoundButton.OnCheckedChangeListener dictCheckboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                dictIndexStore.dictionariesSelectedMap.put(buttonView.getHint().toString(), new DictInfo(buttonView.getHint().toString()));
            } else {
                dictIndexStore.dictionariesSelectedMap.remove(buttonView.getHint().toString());
            }
            enableButtonIfDictsSelected();
        }
    };

    private final CompoundButton.OnCheckedChangeListener indexCheckboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            for (String url : dictIndexStore.indexedDicts.get(buttonView.getHint().toString())) {
                dictCheckBoxes.get(url).setChecked(isChecked);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************");
        layout = findViewById(R.id.get_url_layout);
        sharedDictVersionStore = getSharedPreferences(
                getString(R.string.dict_version_store), Context.MODE_PRIVATE);
        if (dictIndexStore == null) {
            dictIndexStore = (DictIndexStore) getIntent().getSerializableExtra("dictIndexStore");
        }
        Log.d(LOGGER_TAG, ":onCreate:" + "whenActivityLoaded dictIndexStore.indexedDicts " + dictIndexStore.indexedDicts);
        Log.d(LOGGER_TAG, ":onCreate:" + "indexesSelected " + dictIndexStore.indexesSelected.toString());
        setContentView(R.layout.activity_get_url);
        topText = findViewById(R.id.get_url_textView);
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText.setMovementMethod(LinkMovementMethod.getInstance());

        button = findViewById(R.id.get_url_button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);

        setContentView(R.layout.activity_get_url);
        dictIndexStore.getIndexedDictsSetCheckboxes(this);
    }

    // Called when another activity comes inbetween and is dismissed.
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOGGER_TAG, "onResume:" + "************************STARTS****************************");
        this.showNetworkInfo((TextView)findViewById(R.id.get_url_textView2));
    }


    private void enableButtonIfDictsSelected() {
        button = findViewById(R.id.get_url_button);
        button.setEnabled(!dictIndexStore.dictionariesSelectedMap.isEmpty());
        int estimatedSize = dictIndexStore.estimateDictionariesSelectedMBs();
        String message = String.format(getString(R.string.get_dicts_button), dictIndexStore.dictionariesSelectedMap.size(),estimatedSize);
        button.setText(message);
        Log.d(LOGGER_TAG, ":enableButtonIfDictsSelected:" + "button enablement " + button.isEnabled());
    }

    public void buttonPressed1(@SuppressWarnings("UnusedParameters") View v) {
        Intent intent = new Intent(this, GetDictionariesActivity.class);
        intent.putExtra("dictIndexStore", dictIndexStore);
        startActivity(intent);
    }

    // TODO: In certain cases, it could be that the dictdata directory is cleared (eg. by some antivirus). In this case, all dictionaries should be selected.
    private void selectCheckboxes(Set<String> dictionariesSelectedSet) {
        if (dictionariesSelectedSet == null) {
            for (CheckBox cb : dictCheckBoxes.values()) {
                // handle values: kRdanta-rUpa-mAlA -> 2016-02-20_23-22-27
                String filename = cb.getHint().toString();
                boolean proposedVersionNewer = true;

                String[] dictnameParts = DictNameHelper.getDictNameAndVersion(filename);
                String dictname = dictnameParts[0];
                if (sharedDictVersionStore.contains(dictname)) {
                    String currentVersion = sharedDictVersionStore.getString(dictname, getString(R.string.defaultDictVersion));
                    String proposedVersion = getString(R.string.defaultDictVersion);
                    if (dictnameParts.length > 1) {
                        proposedVersion = dictnameParts[1];
                    }
                    //noinspection ComparatorResultComparison
                    proposedVersionNewer = (proposedVersion.compareTo(currentVersion) > 1);
                }

                if (proposedVersionNewer) {
                    cb.setChecked(true);
                } else {
                    cb.setChecked(false);
                    dictIndexStore.autoUnselectedDicts++;
                }
            }
        } else {
            for (CheckBox cb : dictCheckBoxes.values()) {
                String filename = cb.getHint().toString();
                if (dictionariesSelectedSet.contains(filename)) {
                    cb.setChecked(true);
                } else {
                    cb.setChecked(false);
                }
            }
        }

        // checkbox-change listener is only called if there is a change - not if all checkboxes are unselected to start off.
        enableButtonIfDictsSelected();

        @SuppressLint("DefaultLocale") String message = String.format("Based on what we remember installing (%1d dicts) from earlier runs of this app (>=  2.9) on this device, we have auto-unselected ~ %2d dictionaries which don\\'t seem to be new or updated. You can reselect.", sharedDictVersionStore.getAll().size(), dictIndexStore.autoUnselectedDicts);
        topText.append(message);
        Log.d(LOGGER_TAG, ":selectCheckboxes:" +  message);
    }

    void addCheckboxes(Map<String, List<String>> indexedDicts, Set<String> dictionariesSelectedSet) {
        layout = findViewById(R.id.get_url_layout);
        for (String indexName : indexedDicts.keySet()) {
            CheckBox indexBox = new CheckBox(getApplicationContext());
            indexBox.setBackgroundColor(Color.YELLOW);
            indexBox.setTextColor(Color.BLACK);
            indexBox.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            indexBox.setVisibility(View.VISIBLE);
            indexBox.setText(String.format(getString(R.string.fromSomeIndex), indexName));
            indexBox.setHint(indexName);
            indexBox.setOnCheckedChangeListener(indexCheckboxListener);
            indexCheckBoxes.put(indexName, indexBox);
            layout.addView(indexBox);

            for (String url : indexedDicts.get(indexName)) {
                CheckBox cb = new CheckBox(getApplicationContext());
                cb.setText(url.replaceAll(".*/", ""));
                cb.setHint(url);
                cb.setTextColor(Color.BLACK);
                layout.addView(cb, layout.getChildCount());
                cb.setOnCheckedChangeListener(dictCheckboxListener);
                dictCheckBoxes.put(url, cb);
            }
        }

        String message = String.format(getString(R.string.added_n_dictionary_urls), dictCheckBoxes.size());
        Log.i(LOGGER_TAG, ":addCheckboxes:" + message);
        topText = findViewById(R.id.get_url_textView);
        topText.setText(message);
        selectCheckboxes(dictionariesSelectedSet);
    }
}
