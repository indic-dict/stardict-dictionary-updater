package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.io.Files;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

// See comment in MainActivity.java for a rough overall understanding of the code.
public class GetDictionariesActivity extends Activity {
    private SharedPreferences.Editor dictVersionEditor;
    private final ArrayList<String> dictionariesSelectedLst = new ArrayList<>();
    private static final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    private final List<String> dictFiles = new ArrayList<>();
    private List<Boolean> dictFailure = new ArrayList<>();

    private TextView topText;
    private Button button;
    private Button button_2;
    private ProgressBar progressBar;

    private File downloadsDir;
    private File dictDir;


    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedDictVersionStore = getSharedPreferences(
                getString(R.string.dict_version_store), Context.MODE_PRIVATE);
        // Suppressed intellij warning about missing commit. storeDictVersion() has it.
        dictVersionEditor = sharedDictVersionStore.edit();
        setContentView(R.layout.activity_get_dictionaries);
        topText = findViewById(R.id.get_dict_textView);
        button = findViewById(R.id.get_dict_button);
        button.setText(getString(R.string.buttonWorking));
        button.setEnabled(false);
        button_2 = findViewById(R.id.get_dict_button_2);
        button_2.setVisibility(View.INVISIBLE);
        button_2.setEnabled(false);
        progressBar = findViewById(R.id.get_dict_progressBar);
        //noinspection unchecked
        Set<String> dictionariesSelected = (Set<String>) getIntent().getSerializableExtra("dictionariesSelected");
        dictionariesSelectedLst.addAll(dictionariesSelected);
        dictFailure = new ArrayList<>(Collections.nCopies(dictionariesSelectedLst.size(), false));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d(getLocalClassName(), "Got write permissions");
        } else {
            Log.e(getLocalClassName(), "Don't have write permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            Log.i(getLocalClassName(), "new permission: " + ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE));
        }

        File sdcard = Environment.getExternalStorageDirectory();
        downloadsDir = new File(sdcard.getAbsolutePath() + "/Download/dicttars");
        if (!downloadsDir.exists()) {
            if (!downloadsDir.mkdirs()) {
                Log.w(getLocalClassName(), "Returned false while mkdirs " + downloadsDir);
            }
        }
        dictDir = new File(sdcard.getAbsolutePath() + "/dictdata");
        if (!dictDir.exists()) {
            if (!dictDir.mkdirs()) {
                Log.w(getLocalClassName(), "Returned false while mkdirs " + dictDir);
            }
        }

        getDictionaries(0);
    }

    public void buttonPressed1(@SuppressWarnings("UnusedParameters") View v) {
        finish();
    }

    private void getDictionaries(int index) {
        if (dictionariesSelectedLst.size() == 0) {
            topText.setText(R.string.no_dicts_selected);
            topText.append(getString(R.string.txtTryAgain));
            button.setText(R.string.proceed_button);
            button.setEnabled(true);
        } else {
            if (index >= dictionariesSelectedLst.size()) {
                extractDicts(0);
            } else {
                topText.setText(String.format(getString(R.string.gettingSomeDict), dictionariesSelectedLst.get(index)));
                topText.append("\n" + getString(R.string.dont_navigate_away));
                Log.d("downloadDict ", topText.getText().toString());
                downloadDict(index);
            }
        }
    }

    private void whenAllDictsExtracted() {
        topText.setText(getString(R.string.finalMessage));
        List<String> dictNames = Lists.transform(dictionariesSelectedLst, new Function<String, String>() {
            public String apply(String in) {
                return Files.getNameWithoutExtension(in);
            }
        });
        final StringBuilder failures = new StringBuilder("");
        for (int i = 0; i < dictNames.size(); i++) {
            //noinspection StatementWithEmptyBody
            if (dictFailure.get(i)) {
                failures.append("\n").append(dictNames.get(i));
            } else {
            }
        }
        if (failures.length() > 0) topText.append("\n" + "Failed on:" + failures);
        StringBuilder successes = new StringBuilder("");
        for (int i = 0; i < dictNames.size(); i++) {
            //noinspection StatementWithEmptyBody
            if (dictFailure.get(i)) {
            } else {
                successes.append("\n").append(dictNames.get(i));
            }
        }
        if (successes.length() > 0) topText.append("\n" + "Succeeded on:" + successes);

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

        final Activity thisActivity = this;
        button_2.setText(R.string.PROBLEM_SEND_LOG);
        button_2.setVisibility(View.VISIBLE);
        button_2.setEnabled(true);
        button_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ErrorHandler.sendLoagcatMail(thisActivity);
                finishAffinity();
            }
        });

    }

    private void setTopTextWhileExtracting(String archiveName, String contentFileExtracted) {
        String message1 = "Extracting " + archiveName;
        Log.d("DictExtracter", message1);
        topText.setText(message1);
        topText.append("\n" + getString(R.string.dont_navigate_away));
        topText.append("\n" + "Current file: " + contentFileExtracted);
    }

    private void extractDicts(int index) {
        if (index >= dictFiles.size()) {
            whenAllDictsExtracted();
        } else {
            setTopTextWhileExtracting(dictionariesSelectedLst.get(index), "");
            new DictExtractor().execute(index);
        }
    }

    // Log errors, record failure, get the next dictionary.
    private void handleDownloadDictFailure(final int index, String url, Throwable throwable) {
        String message = "Failed to get " + url;
        Log.e("downloadDict", message + ":", throwable);
        topText.setText(message);
        dictFailure.set(index, true);
        getDictionaries(index + 1);
        progressBar.setVisibility(View.GONE);
    }

    private void updateProgressBar(int progress, int total) {
        progressBar.setMax(total);
        progressBar.setProgress(progress);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void downloadDict(final int index) {
        final String url = dictionariesSelectedLst.get(index);
        Log.d("downloadDict", "Getting " + url);
        try {
            final String fileName = url.substring(url.lastIndexOf("/")).replace("/", "");
            if (fileName.isEmpty()) {
                throw new IllegalArgumentException("fileName is empty for url " + url);
            }
            asyncHttpClient.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:32.0) Gecko/20100101 Firefox/32.0");
            asyncHttpClient.setEnableRedirects(true, true, true);
            // URL could be bad, hence the below.
            asyncHttpClient.get(url, new FileAsyncHttpResponseHandler(new File(downloadsDir, fileName)) {
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, File file) {
                    dictFiles.add(fileName);
                    Log.i("Got dictionary: ", fileName);
                    dictFailure.set(index, false);
                    getDictionaries(index + 1);
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    super.onProgress(bytesWritten, totalSize);
                    updateProgressBar((int)bytesWritten, (int)totalSize);
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, File file) {
                    Log.e("downloadDict", "status " + statusCode);
                    handleDownloadDictFailure(index, url, throwable);
                }
            });
        } catch (Throwable throwable) {
            handleDownloadDictFailure(index, url, throwable);
        }
    }

    private void storeDictVersion(String fileName) {
        String[] filenameParts = DictNameHelper.getDictNameAndVersion(fileName);
        final String dictName = filenameParts[0];
        if (filenameParts.length > 1) {
            String dictVersion = Files.getNameWithoutExtension(Files.getNameWithoutExtension(fileName)).split("__")[1];
            dictVersionEditor.putString(dictName, dictVersion);
            dictVersionEditor.commit();
        } else {
            Log.w("DictExtractor", "Storing default dictionary version for " + fileName);
            dictVersionEditor.putString(dictName, getString(R.string.defaultDictVersion));
            dictVersionEditor.commit();
        }
    }

    class DictExtractor extends AsyncTask<Integer, String, Integer> /* params, progress, result */ {
        final CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory(true /*equivalent to setDecompressConcatenated*/);
        final ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();

        void deleteTarFile(String sourceFile) {
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
            progressBar.setVisibility(View.GONE);
        }

        void cleanDirectory(File directory) {
            Log.i("DictExtractor", "Cleaning " + directory);

            File[] files = directory.listFiles();
            Log.d("DictExtractor", "Deleting " + files.length);
            //noinspection ConstantConditions
            if (files == null) {  // null if security restricted
                Log.e("DictExtractor", "Could not list files - got null");
            }

            for (File file : files) {
                if (file.isDirectory()) {
                    cleanDirectory(file);
                }
                Log.d("DictExtractor", "Deleting " + file.getName() + " with result " + file.delete());
            }
        }

        ArchiveInputStream inputStreamFromArchive(String sourceFile) throws FileNotFoundException, CompressorException, ArchiveException {
            // To handle "IllegalArgumentException: Mark is not supported", we wrap with a BufferedInputStream
            // as suggested in http://apache-commons.680414.n4.nabble.com/Compress-Reading-archives-within-archives-td746866.html
            return archiveStreamFactory.createArchiveInputStream(
                    new BufferedInputStream(compressorStreamFactory.createCompressorInputStream(
                            new BufferedInputStream(new FileInputStream(sourceFile))
                    )));
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            updateProgressBar(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
            setTopTextWhileExtracting(values[2], values[3]);
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            int index = params[0];
            String archiveFileName = dictFiles.get(index);
            String sourceFile = new File(downloadsDir.toString(), archiveFileName).getAbsolutePath();

            // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27
            final String baseName = Files.getNameWithoutExtension(Files.getNameWithoutExtension(archiveFileName)).split("__")[0];
            File destDirFile = new File(dictDir.toString(), baseName);
            final String initialDestDir = destDirFile.getAbsolutePath();

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
                ArchiveInputStream archiveInputStream = inputStreamFromArchive(sourceFile);
                int totalFiles = 0;
                while (archiveInputStream.getNextEntry() != null) {
                    totalFiles = totalFiles + 1;
                }
                if (totalFiles == 0) {
                    throw new Exception("0 files in archive??!");
                }

                // Reopen stream
                archiveInputStream = inputStreamFromArchive(sourceFile);

                final byte[] buffer = new byte[50000];
                String baseNameAccordingToArchiveEntries = null;
                ArchiveEntry currentEntry;
                File resourceDirFile = new File(initialDestDir, "res");
                int filesRead = 0;
                while ((currentEntry = archiveInputStream.getNextEntry()) != null) {
                    filesRead = filesRead + 1;
                    String destFileName = String.format("%s.%s", Files.getNameWithoutExtension(currentEntry.getName()), Files.getFileExtension(currentEntry.getName()));
                    String destFileExtension = Files.getFileExtension(destFileName);
                    boolean isResourceFile = !destFileName.isEmpty() && currentEntry.getName().replace(destFileName, "").endsWith("/res/");
                    Log.d("DictExtractor", "isResourceFile " + isResourceFile);
                    String message3 = "Destination: " + destFileName + "\nArchive entry: " + currentEntry.getName();
                    Log.d("DictExtractor", message3);
                    if (isResourceFile && !resourceDirFile.exists()) {
                        Log.i("DictExtractor", "Creating afresh the resource directory " + resourceDirFile.mkdirs());
                    }
                    String destFileDir = initialDestDir;
                    if (isResourceFile) {
                        destFileDir = resourceDirFile.getAbsolutePath();
                    }

                    if (isResourceFile || destFileExtension.matches("ifo|dz|dict|idx|rifo|ridx|rdic")) {
                        String destFile = new File(destFileDir, destFileName).getAbsolutePath();
                        if (!isResourceFile && destFileExtension.matches("ifo|dz|dict|idx|rifo|ridx|rdic")) {
                            String baseNameAccordingToArchiveEntry = DictNameHelper.getNameWithoutAnyExtension(destFileName);
                            if (baseNameAccordingToArchiveEntries == null) {
                                baseNameAccordingToArchiveEntries = baseNameAccordingToArchiveEntry;
                            } else {
                                if (!baseNameAccordingToArchiveEntries.equals(baseNameAccordingToArchiveEntry)) {
                                    throw new Exception("baseNameAccordingToArchiveEntries inconsistent for: " + destFileName + "  Expected " + baseNameAccordingToArchiveEntries);
                                }
                            }
                        }
                        FileOutputStream fos = new FileOutputStream(destFile);
                        int n;
                        while (-1 != (n = archiveInputStream.read(buffer))) {
                            fos.write(buffer, 0, n);
                        }
                        fos.close();
                        publishProgress(Integer.toString(filesRead), Integer.toString(totalFiles), archiveFileName, destFile);
                    } else {
                        Log.w("DictExtractor", "Not extracting " + currentEntry.getName());
                    }
                }
                archiveInputStream.close();
                if (baseNameAccordingToArchiveEntries != null && !baseNameAccordingToArchiveEntries.equals(baseName)) {
                    Log.d("DictExtractor", "baseName: " + baseName + ", baseNameAccordingToArchiveEntries: " + baseNameAccordingToArchiveEntries);
                    final String finalDestDir = new File(dictDir.toString(), baseNameAccordingToArchiveEntries).getAbsolutePath();
                    Log.d("DictExtractor", "destDirFile: " + destDirFile.toString() + ", finalDestDir: " + finalDestDir);
                    final File finalDestDirFile = new File(finalDestDir);
                    if (finalDestDirFile.exists()) {
                        cleanDirectory(finalDestDirFile);
                        Log.w("DictExtractor", "Deleting preexisting dict directory with result: " + finalDestDirFile.delete());
                    }
                    final boolean result = destDirFile.renameTo(finalDestDirFile);
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
        // Don't navigate away in the midst of downloading dictionaries!
        if (button_2.getVisibility() == View.VISIBLE) {
            finish();
        }
    }

}
