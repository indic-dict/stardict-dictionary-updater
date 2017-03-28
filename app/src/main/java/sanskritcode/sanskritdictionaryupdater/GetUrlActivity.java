package sanskritcode.sanskritdictionaryupdater;

import android.app.Activity;
import android.content.Intent;
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
import java.util.List;

// See comment in MainActivity.java for a rough overall understanding of the code.
public class GetUrlActivity extends Activity {
    HashSet<String> indexesSelected = MainActivity.indexesSelected;
    public static List<String> dictUrls = new ArrayList<String>();
    private static final String DICTIONARY_LOCATION = "dict";
    private static final String DOWNLOAD_LOCATION = "dict";

    private TextView topText;
    private Button button;

    public static HashSet<String> dictionariesSelected = new HashSet<String>();
    CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                dictionariesSelected.add(buttonView.getHint().toString());
            } else {
                dictionariesSelected.remove(buttonView.getHint().toString());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_url);
        topText = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);

        setContentView(R.layout.activity_get_url);
        DictUrlGetter dictUrlGetter = new DictUrlGetter();
        dictUrlGetter.execute(indexesSelected.toArray(new String[0]));
    }


    public void buttonPressed1(View v) {
        Button button = (Button) findViewById(R.id.button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);
        Intent intent = new Intent(this, GetDictionariesActivity.class);
        startActivity(intent);
    }

    protected class DictUrlGetter extends AsyncTask<String, Integer, Integer> {
        private final String DICT_URL_GETTER = DictUrlGetter.class.getName();

        @Override
        public Integer doInBackground(String... dictionaryListUrls) {
            Log.i(DICT_URL_GETTER, getString(R.string.use_n_dictionary_indexes) + dictionaryListUrls.length);
            dictUrls = new ArrayList<String>();
            for (String url : dictionaryListUrls) {
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
                    while ((line = r.readLine()) != null) {
                        String dictUrl = line.replace("<", "").replace(">", "");
                        dictUrls.add(dictUrl);
                        Log.d(DICT_URL_GETTER, getString(R.string.added_dictionary_url) + dictUrl);
                        publishProgress(dictUrls.size());
                    }
                } catch (IOException e) {
                    Log.e(DICT_URL_GETTER, "Failed " + e.getStackTrace());
                }
            }
            Log.i(DICT_URL_GETTER, getString(R.string.added_n_dictionary_urls) + dictUrls.size());
            return dictUrls.size();
        }

        // A method used for debugging
        protected void retainOnlyOneDictForDebugging() {
            Log.i(DICT_URL_GETTER, "DEBUGGING!");
            String firstDict = dictUrls.get(0);
            dictUrls.clear();
            dictUrls.add(firstDict);
        }

        @Override
        protected void onPostExecute(Integer result) {
            // retainOnlyOneDictForDebugging();
            String message = R.string.added_n_dictionary_urls + dictUrls.size() + " " +
                    getString(R.string.download_dictionaries);
            topText.setText(message);
            LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
            for (String url : dictUrls) {
                CheckBox cb = new CheckBox(getApplicationContext());
                cb.setText(url.replaceAll(".*/", ""));
                cb.setHint(url);
                cb.setTextColor(Color.BLACK);
                cb.setChecked(true);
                dictionariesSelected.add(url);
                layout.addView(cb, layout.getChildCount());
                cb.setOnCheckedChangeListener(checkboxListener);
            }
            button.setText(getString(R.string.proceed_button));
            button.setEnabled(true);
            // getDictionaries(0);
        }

    }
    @Override
    public void onBackPressed() {
    }
}
