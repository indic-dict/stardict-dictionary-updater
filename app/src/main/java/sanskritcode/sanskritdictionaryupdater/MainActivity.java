package sanskritcode.sanskritdictionaryupdater;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flow: MainActivity::OnCreate ->  IndexGetter -> (user chooses indices, checkboxListener) -> buttonPressed1 ->
 * GetUrlActivity::DictUrlGetter -> (user chooses dictionaries)
 * GetDictionariesActivity -> (getDictionaries <-> downloadDict) -> (extractDict <-> DictExtracter)
 */
public class MainActivity extends Activity {
    private static final String MAIN_ACTIVITY = "MainActivity";
    private static final String[] index_indexorum = {"https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md"};
    public static Map<String, String> indexUrls = new LinkedHashMap<String, String>();
    public static Map<String, String> indexesSelected = new LinkedHashMap<String, String>();

    private TextView topText;
    private Button button;


    CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                indexesSelected.put(buttonView.getText().toString(), buttonView.getHint().toString());
            } else {
                indexesSelected.remove(buttonView.getText().toString());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        topText = (TextView) findViewById(R.id.textView);
        topText.setMovementMethod(new ScrollingMovementMethod());
        button = (Button) findViewById(R.id.button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);
        MainActivity.IndexGetter indexGetter = new MainActivity.IndexGetter();
        indexGetter.execute(index_indexorum);
    }


    public void buttonPressed1(View v) {
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);
        Intent intent = new Intent(this, GetUrlActivity.class);
        // intent.putStringArrayListExtra();
        startActivity(intent);
    }

    protected class IndexGetter extends AsyncTask<String, Integer, Integer> {
        private final String INDEX_GETTER = MainActivity.IndexGetter.class.getName();

        @Override
        public Integer doInBackground(String ... params) {
        public Integer doInBackground(String ... params) {
            String indexList = params[0];
            Log.i(INDEX_GETTER, indexList);
            try {
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpGet httppost = new HttpGet(indexList);
                HttpResponse response = htt
                HttpEntity ht = response.getEntity();

                BufferedHttpEntity buf = new BufferedHttpEntity(ht);

                InputStream is = buf.getContent();
                BufferedReader r = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = r.readLine()) != null) {
                    String url = line.replace("<", "").replace(">", "");
                    String name = url.replaceAll("https://raw.githubusercontent.com/|/tars/tars.MD", "");
                    indexUrls.put(name, url);
                    Log.d(INDEX_GETTER, getString(R.string.added_index_url) + url);
                    publishProgress(indexUrls.size());
                }
            } catch (IOException e) {
                Log.e(INDEX_GETTER, "Failed " + e.getStackTrace());
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            // retainOnlyOneDictForDebugging();
            LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
            for (String name: indexUrls.keySet()) {
                CheckBox cb = new CheckBox(getApplicationContext());
                cb.setText(name);
                cb.setHint(indexUrls.get(name));
                cb.setTextColor(Color.BLACK);
                cb.setChecked(true);
                cb.setOnCheckedChangeListener(checkboxListener);
                indexesSelected.put(name, indexUrls.get(name));
                layout.addView(cb, layout.getChildCount());
            }
            button.setText(getString(R.string.proceed_button));
            button.setEnabled(true);
            // getDictionaries(0);
        }

    }
}
