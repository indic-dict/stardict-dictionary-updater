package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
public class MainActivity extends BaseActivity {
    private static final String ACTIVITY_NAME = "MainActivity";

    private Button button;

    static void getPermission(String permissionString, Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, permissionString) == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permissions", "Got permission: " + permissionString);
        } else {
            Log.w("Permissions", "Don't have permission: " + permissionString);
            ActivityCompat.requestPermissions(activity, new String[]{permissionString}, 101);
            Log.i("Permissions", "new permission: " + ContextCompat.checkSelfPermission(activity, permissionString));
        }
    }

    // Event handler for: When an index is (un) selected.
    private final CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                //noinspection ResultOfMethodCallIgnored
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

    // Called everytime at activity load time, even when back button is pressed - as startActivity(intent) is called.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (dictIndexStore == null){
            dictIndexStore = new DictIndexStore();
        }
        Log.d(ACTIVITY_NAME, "onCreate Indices selected " + dictIndexStore.indexesSelected.toString());
        setContentView(R.layout.activity_main);

        TextView topText = findViewById(R.id.main_textView);
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText.setMovementMethod(LinkMovementMethod.getInstance());

        button = findViewById(R.id.main_button);
        button.setText(getString(R.string.buttonWorking));
        button.setClickable(false);

        MainActivity.getPermission(Manifest.permission.INTERNET, this);
        MainActivity.getPermission(Manifest.permission.ACCESS_NETWORK_STATE, this);
        MainActivity.getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, this);
        dictIndexStore.getIndicesAddCheckboxes(this);
    }

    // Called when another activity comes inbetween and is dismissed.
    @Override
    protected void onResume() {
        super.onResume();
        this.showNetworkInfo((TextView)findViewById(R.id.main_textView2));
        button.setText(R.string.proceed_button);
        button.setClickable(true);
        Log.d(ACTIVITY_NAME, "onResume Indices selected " + dictIndexStore.indexesSelected.toString());
    }

    // Event handler for: When the proceed button is pressed.
    public void buttonPressed1(@SuppressWarnings("UnusedParameters") View v) {
        Log.d(ACTIVITY_NAME, "buttonPressed1 Indices selected " + dictIndexStore.indexesSelected.toString());
        Intent intent = new Intent(this, GetUrlActivity.class);
        intent.putExtra("dictIndexStore", dictIndexStore);
        // intent.putStringArrayListExtra();
        startActivity(intent);
    }
}
