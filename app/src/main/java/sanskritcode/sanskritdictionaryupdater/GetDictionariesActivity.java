package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.Header;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
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

    static private List<CheckBox> checkBoxes = new ArrayList<CheckBox>();
    static private TextView topText;
    static private Button button;
    static private ProgressBar progressBar;

    private File sdcard;
    private File downloadsDir;
    private File dictDir;
    private boolean allDone = false;

    static SharedPreferences sharedDictVersionStore;
    static SharedPreferences.Editor dictVersionEditor;

    public static String[] getDictNameAndVersion(String fileName) {
        // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27.tar.gz
        // Hence calling getBaseName twice.
        return FilenameUtils.getBaseName(FilenameUtils.getBaseName(fileName)).split("__");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedDictVersionStore = getSharedPreferences(
                getString(R.string.dict_version_store), Context.MODE_PRIVATE);
        dictVersionEditor = sharedDictVersionStore.edit();
        allDone = false;
        setContentView(R.layout.activity_get_dictionaries);
        topText = (TextView) findViewById(R.id.get_dict_textView);
        button = (Button) findViewById(R.id.get_dict_button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);
        progressBar = (ProgressBar) findViewById(R.id.get_dict_progressBar);
        dictionariesSelected = (Set<String>) getIntent().getSerializableExtra("dictionariesSelected");
        dictionariesSelectedLst.addAll(dictionariesSelected);
        dictFailure = new ArrayList<Boolean>(Collections.nCopies(dictionariesSelectedLst.size(), false));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(getLocalClassName(), "Got write permissions");
        } else {
            Log.e(getLocalClassName(), "Don't have write permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            Log.i(getLocalClassName(), "new permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE));
        }

        sdcard = Environment.getExternalStorageDirectory();
        downloadsDir = new File(sdcard.getAbsolutePath() + "/Download/dicttars");
        if (downloadsDir.exists() == false) {
            if (!downloadsDir.mkdirs()) {
                Log.w(getLocalClassName(), "Returned false while mkdirs " + downloadsDir);
            }
        }
        dictDir = new File(sdcard.getAbsolutePath() + "/dictdata");
        if (dictDir.exists() == false) {
            if (!dictDir.mkdirs()) {
                Log.w(getLocalClassName(), "Returned false while mkdirs " + dictDir);
            }
        }

        getDictionaries(0);
    }

    public void buttonPressed1(View v) {
        finish();
    }

    protected void getDictionaries(int index) {
        if (dictionariesSelectedLst.size() == 0) {
            topText.setText("No dictionaries selected!");
            topText.append(getString(R.string.txtTryAgain));
            button.setText(R.string.proceed_button);
            button.setEnabled(true);
        } else {
            if (index >= dictionariesSelectedLst.size()) {
                extractDicts(0);
            } else {
                topText.setText("Getting " + dictionariesSelectedLst.get(index));
                topText.append("\n" + getString(R.string.dont_navigate_away));
                Log.d("downloadDict ", topText.getText().toString());
                downloadDict(index);
            }
        }
    }

    protected void whenAllDictsExtracted() {
        topText.setText(getString(R.string.finalMessage));
        List<String> dictNames = Lists.transform(dictionariesSelectedLst, new Function<String, String>() {
            public String apply(String in) {
                return FilenameUtils.getBaseName(in);
            }
        });
        final StringBuffer failures = new StringBuffer("");
        for (int i = 0; i < dictNames.size(); i++) {
            if (dictFailure.get(i)) {
                failures.append("\n" + dictNames.get(i));
            } else {
            }
        }
        if (failures.length() > 0) topText.append("\n" + "Failed on:" + failures);
        StringBuffer successes = new StringBuffer("");
        for (int i = 0; i < dictNames.size(); i++) {
            if (dictFailure.get(i)) {
            } else {
                successes.append("\n" + dictNames.get(i));
            }
        }
        if (successes.length() > 0) topText.append("\n" + "Succeeded on:" + successes);

        button.setEnabled(true);
        button.setText(R.string.buttonValQuit);
        final Activity thisActivity = this;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (failures.length() > 0) {
                    ErrorHandler.sendLoagcatMail(thisActivity);
                }
                finishAffinity();
            }
        });
        return;
    }

    protected void extractDicts(int index) {
        if (index >= dictFiles.size()) {
            whenAllDictsExtracted();
        } else {
            String message1 = "Extracting " + dictionariesSelectedLst.get(index);
            Log.d("DictExtracter", message1);
            topText.setText(message1);
            topText.append("\n" + getString(R.string.dont_navigate_away));
            new DictExtractor().execute(index);
        }
    }

    // Log errors, record failure, get the next dictionary.
    protected void handleDownloadDictFailure(final int index, String url, Throwable throwable) {
        String message = "Failed to get " + url;
        Log.e("downloadDict", message + ":", throwable);
        topText.setText(message);
        dictFailure.set(index, true);
        getDictionaries(index + 1);
        progressBar.setVisibility(View.GONE);
    }

    protected void downloadDict(final int index) {
        final String url = dictionariesSelectedLst.get(index);
        Log.d("downloadDict", "Getting " + url);
        final String fileName = FilenameUtils.getName(url);
        asyncHttpClient.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:32.0) Gecko/20100101 Firefox/32.0");
        asyncHttpClient.setEnableRedirects(true, true, true);
        // URL could be bad, hence the below.
        try {
            asyncHttpClient.get(url, new FileAsyncHttpResponseHandler(new File(downloadsDir, fileName)) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, File response) {
                    dictFiles.add(fileName);
                    Log.i("Got dictionary: ", fileName);
                    dictFailure.set(index, false);
                    getDictionaries(index + 1);
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onRetry(int retryNo) {
                    super.onRetry(retryNo);
                }

                @Override
                public void onProgress(int bytesWritten, int totalSize) {
                    super.onProgress(bytesWritten, totalSize);
                    progressBar.setMax(totalSize);
                    progressBar.setProgress(bytesWritten);
                    progressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                    Log.e("downloadDict", "status " + statusCode);
                    handleDownloadDictFailure(index, url, throwable);
                }
            });
        } catch (Throwable throwable) {
            handleDownloadDictFailure(index, url, throwable);
        }
    }

    void storeDictVersion(String fileName) {
        String[] filenameParts = getDictNameAndVersion(fileName);
        final String dictName = filenameParts[0];
        if (filenameParts.length > 1) {
            String dictVersion = FilenameUtils.getBaseName(FilenameUtils.getBaseName(fileName)).split("__")[1];
            dictVersionEditor.putString(dictName, dictVersion);
            dictVersionEditor.commit();
        } else {
            Log.w("DictExtractor", "Storing default dictionary version for " + fileName);
            dictVersionEditor.putString(dictName, getString(R.string.defaultDictVersion));
            dictVersionEditor.commit();
        }
    }

    protected class DictExtractor extends AsyncTask<Integer, Integer, Integer> {
        CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
        ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();

        protected void deleteTarFile(String sourceFile) {
            String message4 = "Deleting " + sourceFile + " " + new File(sourceFile).delete();
            // topText.append(message4);
            Log.d("DictExtractor", message4);
        }

        @Override
        protected void onPostExecute(Integer result) {
            String fileName = dictFiles.get(result);
            String message1 = "Extracted " + fileName;
            Log.d("DictExtractor", message1);
            topText.setText(message1);
            extractDicts(result + 1);
        }

        protected void cleanDirectory(File directory) {
            Log.i("DictExtractor", "Cleaning " + directory);

            File[] files = directory.listFiles();
            Log.d("DictExtractor", "Deleting " + files.length);
            IOException exception = null;
            if (files == null) {  // null if security restricted
                Log.e("DictExtractor", "Could not list files - got null");
            }

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                Log.d("DictExtractor", "Deleting " + file.getName());
                file.delete();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            int index = params[0];
            String archiveFileName = dictFiles.get(index);
            String sourceFile = FilenameUtils.concat(downloadsDir.toString(), archiveFileName);

            // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27
            final String baseName = FilenameUtils.getBaseName(FilenameUtils.getBaseName(archiveFileName)).split("__")[0];
            final String initialDestDir = FilenameUtils.concat(dictDir.toString(), baseName);
            File destDirFile = new File(initialDestDir);

            Log.d("DictExtractor", "Exists " + destDirFile.exists() + " isDir " + destDirFile.isDirectory());
            if (destDirFile.exists()) {
                Log.i("DictExtractor", "Cleaning " + initialDestDir);
                cleanDirectory(destDirFile);
            } else {
                Log.i("DictExtractor", "Creating afresh the directory " + destDirFile.mkdirs());
            }
            Log.d("DictExtractor", "Exists " + destDirFile.exists() + " isDir " + destDirFile.isDirectory());

            String message2 = "Destination directory " + initialDestDir;
            Log.d("DictExtractor", message2);
            try {
                compressorStreamFactory.setDecompressConcatenated(true);
                // To handle "IllegalArgumentException: Mark is not supported", we wrap with a BufferedInputStream
                // as suggested in http://apache-commons.680414.n4.nabble.com/Compress-Reading-archives-within-archives-td746866.html
                ArchiveInputStream archiveInputStream =
                        archiveStreamFactory.createArchiveInputStream(
                                new BufferedInputStream(compressorStreamFactory.createCompressorInputStream(
                                        new BufferedInputStream(new FileInputStream(sourceFile))
                                )));

                final byte[] buffer = new byte[50000];
                ArchiveEntry currentEntry = null;
                String baseNameAccordingToArchiveEntries = null;
                File resourceDirFile = new File(FilenameUtils.concat(initialDestDir, "res"));
                while ((currentEntry = (ArchiveEntry) archiveInputStream.getNextEntry()) != null) {
                    String destFileName = FilenameUtils.getName(currentEntry.getName());
                    String destFileExtension = FilenameUtils.getExtension(destFileName);
                    boolean isResourceFile = currentEntry.getName().replace(destFileName, "").endsWith("/res/");
                    Log.d("DictExtractor", "isResourceFile " + isResourceFile);
                    if (isResourceFile && !resourceDirFile.exists()) {
                        Log.i("DictExtractor", "Creating afresh the resource directory " + resourceDirFile.mkdirs());
                    }
                    if (isResourceFile || destFileExtension.matches("ifo|dz|dict|idx|rifo|ridx|rdic")) {
                        String destFile = FilenameUtils.concat(initialDestDir, destFileName);
                        if (baseNameAccordingToArchiveEntries == null) {
                            baseNameAccordingToArchiveEntries = FilenameUtils.getBaseName(destFileName);
                        } else {
                            if (!baseNameAccordingToArchiveEntries.equals(FilenameUtils.getBaseName(destFileName)) &&
                                    // Check xyz.dict.dz
                                    !baseNameAccordingToArchiveEntries.equals(FilenameUtils.getBaseName(FilenameUtils.getBaseName(destFileName)))) {
                                throw new Exception("baseNameAccordingToArchiveEntries inconsistent: " + destFileName + " vs expected " + baseNameAccordingToArchiveEntries);
                            }
                        }
                        FileOutputStream fos = new FileOutputStream(destFile);
                        String message3 = "Destination: " + destFile;
                        Log.d("DictExtractor", message3);
                        int n = 0;
                        while (-1 != (n = archiveInputStream.read(buffer))) {
                            fos.write(buffer, 0, n);
                        }
                        fos.close();
                    } else {
                        Log.w("DictExtractor", "Not extracting " + currentEntry.getName());
                    }
                }
                archiveInputStream.close();
                if (baseNameAccordingToArchiveEntries != baseName) {
                    Log.d("DictExtractor", "baseName: " + baseName + ", baseNameAccordingToArchiveEntries: " + baseNameAccordingToArchiveEntries);
                    final String finalDestDir = FilenameUtils.concat(dictDir.toString(), baseNameAccordingToArchiveEntries);
                    final boolean result = destDirFile.renameTo(new File(finalDestDir));
                    Log.w("DictExtractor", "Renaming the dict directory with result: " + result);
                }


                dictFailure.set(index, false);
                Log.d("DictExtractor", "success!");
                storeDictVersion(archiveFileName);
            } catch (Exception e) {
                Log.e("DictExtractor", "IOEx:", e);
                dictFailure.set(index, true);
            }
            deleteTarFile(sourceFile);
            return index;
        }
    }

    @Override
    public void onBackPressed() {
        if (button.getText().toString() == getResources().getString(R.string.buttonValQuit)) {
            finish();
        }
    }

}
