package sanskritcode.sanskritdictionaryupdater;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Flow: MainActivity::OnCreate ->  IndexGetter -> (user chooses indices, checkboxListener) -> buttonPressed1 ->
 * GetUrlActivity::DictUrlGetter -> (user chooses dictionaries)
 * GetDictionariesActivity -> (getDictionaries <-> downloadDict) -> (extractDict <-> DictExtracter)
 * <p>
 * IntraActivity lifecycle looks like this: http://stackoverflow.com/questions/6509791/onrestart-vs-onresume-android-lifecycle-question
 */
public class MainActivity extends Activity {
    private static final String MAIN_ACTIVITY = "MainActivity";
    private static final String index_indexorum = "https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md";
    public static Map<String, String> indexUrls = new LinkedHashMap<String, String>();
    public static Map<String, String> indexesSelected = new LinkedHashMap<String, String>();
    protected static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

    static private TextView topText;
    static private Button button;
    static private List<CheckBox> checkBoxes = new ArrayList<CheckBox>();

    CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                indexesSelected.put(buttonView.getText().toString(), buttonView.getHint().toString());
            } else {
                indexesSelected.remove(buttonView.getText().toString());
            }
            button.setEnabled(!indexesSelected.isEmpty());
        }
    };

    // Add checkboxes from indexUrls
    private void addCheckboxes() {
        // retainOnlyOneDictForDebugging();
        checkBoxes = new ArrayList<CheckBox>();
        LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);
        for (String name : indexUrls.keySet()) {
            CheckBox cb = new CheckBox(getApplicationContext());
            cb.setText(name);
            cb.setHint(indexUrls.get(name));
            cb.setTextColor(Color.BLACK);
            cb.setOnCheckedChangeListener(checkboxListener);
            layout.addView(cb, layout.getChildCount());
            checkBoxes.add(cb);
        }
        for (CheckBox checkBox : checkBoxes) {
            if (indexesSelected.keySet().contains(checkBox.getText())) {
                checkBox.setChecked(true);
            } else {
                checkBox.setChecked(false);
            }
        }

        button.setText(getString(R.string.proceed_button));
        // getDictionaries(0);

    }

    // Called everytime at activity load time, even when back button is pressed - as startActivity(intent) is called.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(MAIN_ACTIVITY, "onCreate Indices selected " + indexesSelected.toString());
        setContentView(R.layout.activity_main);

        topText = (TextView) findViewById(R.id.main_textView);
        topText.setMovementMethod(new ScrollingMovementMethod());

        button = (Button) findViewById(R.id.main_button);
        button.setText(getString(R.string.buttonWorking));
        button.setClickable(false);
        getIndices();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(MAIN_ACTIVITY, "OnStart Indices selected " + indexesSelected.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        button.setText(R.string.proceed_button);
        button.setClickable(true);
        Log.d(MAIN_ACTIVITY, "onResume Indices selected " + indexesSelected.toString());
    }

    public void buttonPressed1(View v) {
        Log.d(MAIN_ACTIVITY, "buttonPressed1 Indices selected " + indexesSelected.toString());
        Intent intent = new Intent(this, GetUrlActivity.class);
        // intent.putStringArrayListExtra();
        startActivity(intent);
    }

    void getIndices() {
        asyncHttpClient.get(index_indexorum, new TextHttpResponseHandler() {
            private String CLASS_NAME = this.getClass().getName();
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e(CLASS_NAME, throwable.toString());
            }

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                super.onProgress(bytesWritten, totalSize);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                for (String line: responseString.split("\n")) {
                    String url = line.replace("<", "").replace(">", "");
                    String name = url.replaceAll("https://raw.githubusercontent.com/|/tars/tars.MD", "");
                    indexUrls.put(name, url);
                    indexesSelected.put(name, url);
                    Log.d(CLASS_NAME, getString(R.string.added_index_url) + url);
                }
                addCheckboxes();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Log.d(MAIN_ACTIVITY, "onBack Indices selected " + indexesSelected.toString());
    }
}
