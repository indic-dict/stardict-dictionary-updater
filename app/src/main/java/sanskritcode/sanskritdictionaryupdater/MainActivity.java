package sanskritcode.sanskritdictionaryupdater;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;

/**
 * Flow: OnCreate -> buttonPressed1 -> DictUrlGetter -> (getDictionaries <-> downloadDict) -> (extractDict <-> DictExtracter)
 */
public class MainActivity extends Activity {
    private static final String MAIN_ACTIVITY = "MainActivity";
    private static final String[] available_indexes = {
            "https://raw.githubusercontent.com/sanskrit-coders/stardict-sanskrit/master/sa-head/tars/tars.MD",
            "https://raw.githubusercontent.com/sanskrit-coders/stardict-sanskrit/master/en-head/tars/tars.MD",
            "https://raw.githubusercontent.com/sanskrit-coders/stardict-kannada/master/en-head/tars/tars.MD",
            "https://raw.githubusercontent.com/sanskrit-coders/stardict-kannada/master/kn-head/tars/tars.MD",
            "https://raw.githubusercontent.com/sanskrit-coders/stardict-pali/master/en-head/tars/tars.MD",
    };


    private TextView topText;
    private Button button;

    public static HashSet<String> indexesSelected = new HashSet<String>();
    CompoundButton.OnCheckedChangeListener checkboxListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                indexesSelected.add(buttonView.getText().toString());
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

        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        for (int i = 0; i < available_indexes.length; i++) {
            final String dictionary = available_indexes[i];
            CheckBox cb = new CheckBox(getApplicationContext());
            cb.setText(dictionary);
            cb.setTextColor(Color.BLACK);
            cb.setChecked(true);
            cb.setOnCheckedChangeListener(checkboxListener);
            indexesSelected.add(dictionary);
            layout.addView(cb, layout.getChildCount());
        }

    }


    public void buttonPressed1(View v) {
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);
        Intent intent = new Intent(this, GetUrlActivity.class);
        // intent.putStringArrayListExtra();
        startActivity(intent);
    }

}
