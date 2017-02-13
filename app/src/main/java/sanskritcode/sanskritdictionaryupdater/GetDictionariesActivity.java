package sanskritcode.sanskritdictionaryupdater;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.Header;
import org.apache.http.client.params.ClientPNames;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

// See comment in MainActivity.java for a rough overall understanding of the code.
public class GetDictionariesActivity extends Activity {
    private Set<String> dictionariesSelected = GetUrlActivity.dictionariesSelected;
    private ArrayList<String> dictionariesSelectedLst = new ArrayList<String>();
    protected static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    private List<String> dictFiles = new ArrayList<String>();
    private List<Boolean> dictFailure = new ArrayList<Boolean>();

    private TextView topText;
    private Button button;

    private File sdcard;
    private File downloadsDir;
    private File dictDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_dictionaries);
        topText = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);
        dictionariesSelectedLst.addAll(dictionariesSelected);
        dictFailure = new ArrayList<Boolean>(Collections.nCopies(dictionariesSelectedLst.size(), false));

        sdcard = Environment.getExternalStorageDirectory();
        downloadsDir = new File (sdcard.getAbsolutePath() + "/Download/dicttars");
        if(downloadsDir.exists()==false) {
            downloadsDir.mkdirs();
        }
        dictDir = new File (sdcard.getAbsolutePath() + "/dictdata");
        if(dictDir.exists()==false) {
            dictDir.mkdirs();
        }
        asyncHttpClient.getHttpClient().getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
        getDictionaries(0);
    }

    public void buttonPressed1(View v) {
        Button button = (Button) findViewById(R.id.button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    protected void getDictionaries(int index) {
        if(dictionariesSelectedLst.size() == 0) {
            topText.setText("No dictionaries selected!");
            topText.append(getString(R.string.txtTryAgain));
            button.setText(R.string.proceed_button);
            button.setEnabled(true);
        } else {
            if(index >= dictionariesSelectedLst.size()) {
                extractDicts(0);
            } else {
                topText.setText("Getting " + dictionariesSelectedLst.get(index));
                topText.append("\n" + getString(R.string.dont_navigate_away));
                Log.d("downloadDict", topText.getText().toString());
                downloadDict(index);
            }
        }
    }

    protected void extractDicts(int index) {
        if(index >= dictFiles.size()) {
            topText.setText(getString(R.string.finalMessage));
            List<String> dictNames = Lists.transform(dictionariesSelectedLst, new Function<String, String>() {
                public String apply(String in) {
                    return FilenameUtils.getBaseName(in);
                }
            });
            StringBuffer failures = new StringBuffer("");
            for(int i = 0; i < dictNames.size(); i++) {
                if(dictFailure.get(i)) {
                    failures.append("\n" + dictNames.get(i));
                } else {
                }
            }
            if(failures.length() > 0) topText.append("\n" + "Failed on:" + failures);
            StringBuffer successes = new StringBuffer("");
            for(int i = 0; i < dictNames.size(); i++) {
                if(dictFailure.get(i)) {
                } else {
                    successes.append("\n" + dictNames.get(i));
                }
            }
            if(successes.length() > 0) topText.append("\n" + "Succeeded on:" + successes);

            button.setVisibility(View.GONE);
            return;
        } else {
            String message1 = "Extracting " + dictionariesSelectedLst.get(index);
            Log.d("DictExtracter", message1);
            topText.setText(message1);
            topText.append("\n" + getString(R.string.dont_navigate_away));
            new DictExtracter().execute(index);
        }
    }

    protected void downloadDict(final int index) {
        final String url = dictionariesSelectedLst.get(index);
        final String fileName = FilenameUtils.getName(url);
        asyncHttpClient.get(url, new FileAsyncHttpResponseHandler(new File(downloadsDir, fileName)) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                dictFiles.add(fileName);
                dictFailure.set(index,false);
                getDictionaries(index + 1);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                String message = "Failed to get " + fileName;
                topText.setText(message);
                Log.w("downloadDict", message + ":" + throwable.getStackTrace().toString());
                dictFailure.set(index,true);
                getDictionaries(index + 1);
            }
        });
    }

    protected class DictExtracter extends AsyncTask<Integer, Integer, Integer> {

        protected void deleteTarFile(String sourceFile) {
            String message4 = "Deleting " + sourceFile + " " + new File(sourceFile).delete();
            // topText.append(message4);
            Log.d("DictExtracter", message4);

        }
        @Override
        protected void onPostExecute(Integer result) {
            String fileName = dictFiles.get(result);
            String message1 = "Extracted " + fileName;
            Log.d("DictExtracter", message1);
            topText.setText(message1);
            extractDicts(result + 1);
        }
        @Override
        protected Integer doInBackground(Integer... params) {
            int index = params[0];
            String fileName = dictFiles.get(index);
            String sourceFile = FilenameUtils.concat(downloadsDir.toString(), fileName);

            // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27
            final String baseName = FilenameUtils.getBaseName(FilenameUtils.getBaseName(fileName)).split("__")[0];
            final String destDir = FilenameUtils.concat(dictDir.toString(), baseName);
            File destDirFile = new File(destDir);
            destDirFile.mkdirs();
            try {
                FileUtils.cleanDirectory(destDirFile);
            } catch (IOException e) {
                Log.e("DictExtracter", "Error while cleaning " + destDir);
                Log.e("DictExtracter", e.toString());
            }
            String message2 = "Destination directory " + destDir;
            Log.d("DictExtracter", message2);
            try {
                TarArchiveInputStream tarInput =
                        new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(sourceFile)));

                final byte[] buffer = new byte[50000];
                TarArchiveEntry currentEntry = null;
                while((currentEntry = (TarArchiveEntry) tarInput.getNextEntry()) != null) {
                    String destFile = FilenameUtils.concat(destDir, currentEntry.getName());
                    FileOutputStream fos = new FileOutputStream(destFile);
                    String message3 = "Destination: " + destFile;
                    Log.d("DictExtracter", message3);
                    int n = 0;
                    while (-1 != (n = tarInput.read(buffer))) {
                        fos.write(buffer, 0, n);
                    }
                    fos.close();
                }
                tarInput.close();
                dictFailure.set(index,false);
            } catch (Exception e) {
                Log.w("DictExtracter", "IOEx:" + e.getStackTrace());
                dictFailure.set(index,true);
            }
            deleteTarFile(sourceFile);
            return index;
        }
    }

    @Override
    public void onBackPressed() {
    }

}
