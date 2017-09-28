package sanskritcode.sanskritdictionaryupdater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// See comment in MainActivity.java for a rough overall understanding of the code.
public class GetUrlActivity extends Activity {
    private static final String ACTIVITY_NAME = "GetUrlActivity";
    private static Map<String, String> indexesSelected = MainActivity.indexesSelected;
    private static Map<String, List<String>> indexedDicts = new LinkedHashMap<>();
    private static final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

    private LinearLayout layout;
    private TextView topText;
    private Button button;
    static private final List<CheckBox> checkBoxes = new ArrayList<>();

    private SharedPreferences sharedDictVersionStore;

    public static HashSet<String> dictionariesSelected = new HashSet<>();
    private final CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                dictionariesSelected.add(buttonView.getHint().toString());
            } else {
                dictionariesSelected.remove(buttonView.getHint().toString());
            }
            enableButtonIfDictsSelected();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getLocalClassName(), "whenActivityLoaded indexedDicts " + indexedDicts);

        layout = (LinearLayout) findViewById(R.id.get_url_layout);
        sharedDictVersionStore = getSharedPreferences(
                getString(R.string.dict_version_store), Context.MODE_PRIVATE);

        //noinspection unchecked
        indexesSelected = (Map<String, String>) getIntent().getSerializableExtra("indexesSelected");
        Log.d(getLocalClassName(), "indexesSelected " + indexesSelected.toString());
        indexedDicts = new LinkedHashMap<>();
        dictionariesSelected = new HashSet<>();
        setContentView(R.layout.activity_get_url);
        topText = (TextView) findViewById(R.id.get_url_textView);

        button = (Button) findViewById(R.id.get_url_button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);

        setContentView(R.layout.activity_get_url);
        getDictUrls();
    }

    private void enableButtonIfDictsSelected() {
        button = (Button) findViewById(R.id.get_url_button);
        button.setEnabled(!dictionariesSelected.isEmpty());
        Log.d(ACTIVITY_NAME, "button enablement " + button.isEnabled());
    }

    public void buttonPressed1(@SuppressWarnings("UnusedParameters") View v) {
        Intent intent = new Intent(this, GetDictionariesActivity.class);
        intent.putExtra("dictionariesSelected", dictionariesSelected);
        startActivity(intent);
    }

    private void selectCheckboxes() {
        int autoUnselectedDicts = 0;
        for (CheckBox cb : checkBoxes) {
            // handle values: kRdanta-rUpa-mAlA -> 2016-02-20_23-22-27
            String filename = cb.getHint().toString();
            boolean proposedVersionNewer = true;

            String[] dictnameParts = GetDictionariesActivity.getDictNameAndVersion(filename);
            String dictname = dictnameParts[0];
            if (sharedDictVersionStore.contains(dictname)) {
                String currentVersion = sharedDictVersionStore.getString(dictname, getString(R.string.defaultDictVersion));
                String proposedVersion = getString(R.string.defaultDictVersion);
                if (dictnameParts.length > 1) {
                    proposedVersion = dictnameParts[1];
                }
                proposedVersionNewer = (proposedVersion.compareTo(currentVersion) > 1);
            }

            if (proposedVersionNewer) {
                cb.setChecked(true);
            } else {
                cb.setChecked(false);
                autoUnselectedDicts++;
            }
        }
        // checkbox-change listener is only called if there is a change - not if all checkboxes are unselected to start off.
        enableButtonIfDictsSelected();

        String message = String.format(getString(R.string.autoUnselectedDicts), sharedDictVersionStore.getAll().size(), autoUnselectedDicts);
        topText.append(message);
        Log.d(ACTIVITY_NAME, message);
    }

    private static String getIndexNameFromUrl(String url) {
        for (String key : indexesSelected.keySet()) {
            if (indexesSelected.get(key).equals(url)) {
                return key;
            }
        }
        return null;
    }

    // Populates indexedDicts
    private void getDictUrls() {
        indexedDicts = new LinkedHashMap<>();
        Log.i(ACTIVITY_NAME, getString(R.string.use_n_dictionary_indexes) + indexesSelected.size());
        asyncHttpClient.setEnableRedirects(true, true, true);
        for (String name : indexesSelected.keySet()) {
            final String url = indexesSelected.get(name);
            try {
                asyncHttpClient.get(url, new TextHttpResponseHandler() {
                    final String LOGGER_NAME = "getDictUrls";

                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable){
                        Log.e(LOGGER_NAME, "Failed ", throwable);
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        List<String> urls = new ArrayList<>();
                        for (String line : responseString.split("\n")) {
                            String dictUrl = line.replace("<", "").replace(">", "");
                            urls.add(dictUrl);
                            Log.d(LOGGER_NAME, getString(R.string.added_dictionary_url) + dictUrl);
                        }
                        Log.d(LOGGER_NAME, "Index handled: " + url);
                        indexedDicts.put(getIndexNameFromUrl(url), urls);

                        if (indexesSelected.size() == indexedDicts.size()) {
                            addCheckboxes();
                        }
                    }
                });
            } catch (Throwable throwable) {
                Log.e(ACTIVITY_NAME, "error with " + url, throwable);
                ErrorHandler.sendLoagcatMail(this);
            }
        }
    }

    private void addCheckboxes() {
        layout = (LinearLayout) findViewById(R.id.get_url_layout);
        for (String indexName : indexedDicts.keySet()) {
            TextView text = new TextView(getApplicationContext());
            text.setBackgroundColor(Color.YELLOW);
            text.setTextColor(Color.BLACK);
            text.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            text.setVisibility(View.VISIBLE);
            text.setText("From " + indexName);
            layout.addView(text);

            for (String url : indexedDicts.get(indexName)) {
                CheckBox cb = new CheckBox(getApplicationContext());
                cb.setText(url.replaceAll(".*/", ""));
                cb.setHint(url);
                cb.setTextColor(Color.BLACK);
                layout.addView(cb, layout.getChildCount());
                cb.setOnCheckedChangeListener(checkboxListener);
                checkBoxes.add(cb);
            }
        }

        String message = String.format(getString(R.string.added_n_dictionary_urls), checkBoxes.size());
        Log.i(ACTIVITY_NAME, message);
        topText = (TextView) findViewById(R.id.get_url_textView);
        topText.setText(message);
        selectCheckboxes();
        button.setText(getString(R.string.proceed_button));
    }

    @Override
    public void onBackPressed() {
        indexedDicts = new LinkedHashMap<>();
        finish();
    }
}
