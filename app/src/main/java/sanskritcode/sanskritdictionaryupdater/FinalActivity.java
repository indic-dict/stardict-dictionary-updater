package sanskritcode.sanskritdictionaryupdater;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.util.List;

public class FinalActivity extends BaseActivity {
    final String LOGGER_TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOGGER_TAG, "onCreate:" + "************************STARTS****************************");
        if (dictIndexStore == null) {
            dictIndexStore = (DictIndexStore) getIntent().getSerializableExtra("dictIndexStore");
        }
        setContentView(R.layout.activity_final);
        TextView topText = findViewById(R.id.final_act_textView);
        // For clickable links. See https://stackoverflow.com/a/20647011/444644
        topText.setMovementMethod(LinkMovementMethod.getInstance());
        topText.setText(getString(R.string.finalMessage));
        largeLog(LOGGER_TAG, ":onCreate:" + dictIndexStore.toString());
        final StringBuilder failures = new StringBuilder("");
        for (DictInfo dictInfo: dictIndexStore.dictionariesSelectedMap.values()) {
            //noinspection StatementWithEmptyBody
            if (dictInfo.status != DictStatus.EXTRACTION_SUCCESS ) {
                failures.append("\n").append(DictNameHelper.getNameWithoutAnyExtension(dictInfo.url));
            }
        }
        if (failures.length() > 0) {
            topText.append("\n" + "Failed on:" + failures);
            Log.w(LOGGER_TAG, ":onCreate:" + topText.getText().toString());
        }
        StringBuilder successes = new StringBuilder("");
        for (DictInfo dictInfo: dictIndexStore.dictionariesSelectedMap.values()) {
            //noinspection StatementWithEmptyBody
            if (dictInfo.status == DictStatus.EXTRACTION_SUCCESS) {
                successes.append("\n").append(DictNameHelper.getNameWithoutAnyExtension(dictInfo.url));
            }
        }
        if (successes.length() > 0) {
            topText.append("\n" + "Succeeded on:" + successes);
            Log.w(LOGGER_TAG,":onCreate:" +  topText.getText().toString());
        }

        Button button = findViewById(R.id.final_act_button);
        button.setText(R.string.buttonValQuit);
        if (failures.length() == 0) {
            button.setEnabled(true);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishAffinity();
                }
            });
        } else {
            button.setEnabled(false);
        }

        final BaseActivity thisActivity = this;
        Button button_2 = findViewById(R.id.final_act_button_2);
        button_2.setText(R.string.PROBLEM_SEND_LOG);
        button_2.setVisibility(View.VISIBLE);
        button_2.setEnabled(true);
        button_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thisActivity.sendLoagcatMail();
                finishAffinity();
            }
        });
        Log.i(LOGGER_TAG, "onCreate: version: " + getVersion());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, GetUrlActivity.class);
        intent.putExtra("dictIndexStore", dictIndexStore);
        // intent.putStringArrayListExtra();
        startActivity(intent);
    }

    public void buttonPressed1(@SuppressWarnings("UnusedParameters") View v) {
        finish();
    }
}
