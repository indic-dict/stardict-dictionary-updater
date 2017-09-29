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

import com.loopj.android.http.TextHttpResponseHandler;

import java.util.ArrayList;
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
    DictIndexStore dictIndexStore = new DictIndexStore();

    private Button button;

    // Event handler for: When an index is (un) selected.
    private final CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                dictIndexStore.indexesSelected.put(buttonView.getText().toString(), buttonView.getHint().toString());
            } else {
                dictIndexStore.indexesSelected.remove(buttonView.getText().toString());
            }
            button.setEnabled(!dictIndexStore.indexesSelected.isEmpty());
        }
    };

    // Add checkboxes from indexUrls
    void addCheckboxes(Map<String, String> indexUrls, Map<String, String> indexesSelected) {
        // retainOnlyOneDictForDebugging();
        List<CheckBox> checkBoxes = new ArrayList<>();
        LinearLayout layout = findViewById(R.id.main_layout);
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
            if (indexesSelected.keySet().contains(checkBox.getText().toString())) {
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

        TextView warningText = findViewById(R.id.main_textView2);
        warningText.setBackgroundColor(Color.LTGRAY);
        warningText.setTextColor(Color.RED);
        if (isConnected) {
            if (isWiFi) {
                warningText.setVisibility(View.INVISIBLE);
            } else {
                warningText.setText(R.string.connected_nowifi);
                warningText.setVisibility(View.VISIBLE);
            }
        } else {
            warningText.setText(R.string.noConnection);
            warningText.setVisibility(View.VISIBLE);
        }

    }

    // Called everytime at activity load time, even when back button is pressed - as startActivity(intent) is called.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(MAIN_ACTIVITY, "onCreate Indices selected " + dictIndexStore.indexesSelected.toString());
        setContentView(R.layout.activity_main);

        TextView topText = findViewById(R.id.main_textView);
        topText.setMovementMethod(LinkMovementMethod.getInstance());

        button = findViewById(R.id.main_button);
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

        dictIndexStore.getIndicesAddCheckboxes(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showNetworkInfo();
        button.setText(R.string.proceed_button);
        button.setClickable(true);
        Log.d(MAIN_ACTIVITY, "onResume Indices selected " + dictIndexStore.indexesSelected.toString());
    }

    // Event handler for: When the proceed button is pressed.
    public void buttonPressed1(@SuppressWarnings("UnusedParameters") View v) {
        Log.d(MAIN_ACTIVITY, "buttonPressed1 Indices selected " + dictIndexStore.indexesSelected.toString());
        Intent intent = new Intent(this, GetUrlActivity.class);
        intent.putExtra("dictIndexStore", dictIndexStore);
        // intent.putStringArrayListExtra();
        startActivity(intent);
    }
}
