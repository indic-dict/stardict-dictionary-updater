package sanskritcode.sanskritdictionaryupdater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// See comment in MainActivity.java for a rough overall understanding of the code.
public class GetUrlActivity extends Activity {
    private static final String ACTIVITY_NAME = "GetUrlActivity";
    public static Map<String, String> indexesSelected = MainActivity.indexesSelected;
    public static Map<String, List<String>> indexedDicts = new LinkedHashMap<String, List<String>>();
    private static final String DICTIONARY_LOCATION = "dict";
    private static final String DOWNLOAD_LOCATION = "dict";

    private TextView topText;
    private Button button;

    SharedPreferences sharedDictVersionStore;
    private List<CheckBox> checkBoxes = new ArrayList<CheckBox>();

    public static HashSet<String> dictionariesSelected = new HashSet<String>();
    CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            dictionariesSelected.add(buttonView.getHint().toString());
        } else {
            dictionariesSelected.remove(buttonView.getHint().toString());
        }
        button.setEnabled(dictionariesSelected.size() > 0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getLocalClassName(), "whenActivityLoaded indexedDicts " + indexedDicts);

        sharedDictVersionStore =  getSharedPreferences(
                getString(R.string.dict_version_store), Context.MODE_PRIVATE);

        indexesSelected = MainActivity.indexesSelected;
        indexedDicts = new LinkedHashMap<String, List<String>>();
        dictionariesSelected = new HashSet<String>();
        setContentView(R.layout.activity_get_url);
        topText = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);

        setContentView(R.layout.activity_get_url);
        DictUrlGetter dictUrlGetter = new DictUrlGetter();
        dictUrlGetter.execute(indexesSelected.keySet().toArray(new String[0]));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        button.setText(getString(R.string.proceed_button));
        button.setEnabled(true);
    }

    public void buttonPressed1(View v) {
        Button button = (Button) findViewById(R.id.button);
        Intent intent = new Intent(this, GetDictionariesActivity.class);
        startActivity(intent);
    }

    protected void selectCheckboxes() {
        boolean currentVersionsKnown = (sharedDictVersionStore.getAll().size() > 0);
        int autoUnselectedDicts = 0;
        for(CheckBox cb : checkBoxes) {
            // handle values: kRdanta-rUpa-mAlA -> 2016-02-20_23-22-27
            String filename = cb.getHint().toString();
            boolean proposedVersionNewer = true;

            String[] dictnameParts = GetDictionariesActivity.getDictNameAndVersion(filename);
            String dictname = dictnameParts[0];
            if(dictnameParts.length  > 1) {
                String currentVersion = sharedDictVersionStore.getString(dictname, "0000");
                String proposedVersion = dictnameParts[1];
                proposedVersionNewer = (proposedVersion.compareTo(currentVersion) > 1);
            }

            if (proposedVersionNewer) {
                cb.setChecked(true);
            } else {
                autoUnselectedDicts++;
            }
        }

        String message = String.format(getString(R.string.autoUnselectedDicts), sharedDictVersionStore.getAll().size(), autoUnselectedDicts);
        topText.append(message);
        Log.d(ACTIVITY_NAME, topText.getText().toString());
        Log.d(ACTIVITY_NAME, message);
    }

    protected class DictUrlGetter extends AsyncTask<String, Integer, Integer> {
        private final String DICT_URL_GETTER = DictUrlGetter.class.getName();
        private int dictsRetreieved = 0;

        @Override
        public Integer doInBackground(String... dictionaryListNames) {
            Log.i(DICT_URL_GETTER, getString(R.string.use_n_dictionary_indexes) + dictionaryListNames.length);
            indexedDicts = new LinkedHashMap<String, List<String>>();
            dictsRetreieved = 0;
            for (String name : dictionaryListNames) {
                String url = indexesSelected.get(name);
                Log.i(DICT_URL_GETTER, url);
                try {
                    DefaultHttpClient httpclient = new DefaultHttpClient();
                    HttpGet httppost = new HttpGet(url);
                    HttpResponse response = httpclient.execute(httppost);
                    HttpEntity ht = response.getEntity();

                    BufferedHttpEntity buf = new BufferedHttpEntity(ht);

                    InputStream is = buf.getContent();
                    BufferedReader r = new BufferedReader(new InputStreamReader(is));

                    String line;
                    List<String> urls = new ArrayList<String>();
                    while ((line = r.readLine()) != null) {
                        String dictUrl = line.replace("<", "").replace(">", "");
                        urls.add(dictUrl);
                        Log.d(DICT_URL_GETTER, getString(R.string.added_dictionary_url) + dictUrl);
                        dictsRetreieved++;
                        publishProgress(dictsRetreieved);
                    }
                    indexedDicts.put(name, urls);
                } catch (IOException e) {
                    Log.e(DICT_URL_GETTER, "Failed " + e.getStackTrace());
                }
            }
            Log.i(DICT_URL_GETTER, getString(R.string.added_n_dictionary_urls) + dictsRetreieved);
            return dictsRetreieved;
        }

        @Override
        protected void onPostExecute(Integer result) {
            // retainOnlyOneDictForDebugging();
            String message = String.format(getString(R.string.added_n_dictionary_urls), dictsRetreieved);
            topText.setText(message);
            LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
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

                for(String url: indexedDicts.get(indexName)) {
                    CheckBox cb = new CheckBox(getApplicationContext());
                    cb.setText(url.replaceAll(".*/", ""));
                    cb.setHint(url);
                    cb.setTextColor(Color.BLACK);
                    layout.addView(cb, layout.getChildCount());
                    cb.setOnCheckedChangeListener(checkboxListener);
                    checkBoxes.add(cb);
                }
            }
            selectCheckboxes();
            button.setText(getString(R.string.proceed_button));
            // getDictionaries(0);
        }

    }
    @Override
    public void onBackPressed() {
        indexedDicts = new LinkedHashMap<String, List<String>>();
        finish();
    }
}
