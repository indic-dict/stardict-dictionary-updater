package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
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
    public static LinkedHashMap<String, String> indexesSelected = new LinkedHashMap<String, String>();
    protected static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

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

    private void showNetworkInfo() {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        boolean isWiFi = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;

        TextView warningText = (TextView) findViewById(R.id.main_textView2);
        warningText.setBackgroundColor(Color.LTGRAY);
        warningText.setTextColor(Color.RED);
        if (isConnected) {
            if (isWiFi) {
                warningText.setVisibility(View.INVISIBLE);
            } else {
                warningText.setText("Alert: Connected, but not on Wifi.");
                warningText.setVisibility(View.VISIBLE);
            }
        } else {
            warningText.setText("No data connection. Please retry later.");
            warningText.setVisibility(View.VISIBLE);
        }

    }

    // Called everytime at activity load time, even when back button is pressed - as startActivity(intent) is called.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(MAIN_ACTIVITY, "onCreate Indices selected " + indexesSelected.toString());
        setContentView(R.layout.activity_main);

        TextView topText = (TextView) findViewById(R.id.main_textView);
        topText.setMovementMethod(LinkMovementMethod.getInstance());

        button = (Button) findViewById(R.id.main_button);
        button.setText(getString(R.string.buttonWorking));
        button.setClickable(false);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
            Log.d(getLocalClassName(), "Got INTERNET permissions");
        } else {
            Log.e(getLocalClassName(), "Don't have INTERNET permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 102);
            Log.i(getLocalClassName(), "new permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET));
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(getLocalClassName(), "Got ACCESS_NETWORK_STATE permissions");
        } else {
            Log.e(getLocalClassName(), "Don't have ACCESS_NETWORK_STATE permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 103);
            Log.i(getLocalClassName(), "new permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE));
        }

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
        showNetworkInfo();
        button.setText(R.string.proceed_button);
        button.setClickable(true);
        Log.d(MAIN_ACTIVITY, "onResume Indices selected " + indexesSelected.toString());
    }

    public void buttonPressed1(View v) {
        Log.d(MAIN_ACTIVITY, "buttonPressed1 Indices selected " + indexesSelected.toString());
        Intent intent = new Intent(this, GetUrlActivity.class);
        intent.putExtra("indexesSelected", indexesSelected);
        // intent.putStringArrayListExtra();
        startActivity(intent);
    }

    void getIndices() {
        asyncHttpClient.setEnableRedirects(true, true, true);
        asyncHttpClient.get(index_indexorum, new TextHttpResponseHandler() {
            private String CLASS_NAME = this.getClass().getName();
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e(CLASS_NAME, "getIndices", throwable);
            }

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                super.onProgress(bytesWritten, totalSize);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                for (String line: responseString.split("\n")) {
                    String url = line.replace("<", "").replace(">", "");
                    String name = url.replaceAll("https://raw.githubusercontent.com/|/tars/tars.MD|master/", "");
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
