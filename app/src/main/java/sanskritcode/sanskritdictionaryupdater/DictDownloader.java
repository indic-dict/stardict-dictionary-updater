package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Expected flow: downloadDict(0) -> ... -> downloadDict(index) ->  downloadDict(index + 1) -> ...
 *
 * Why are we using recursion?
 *   - FileAsyncHttpResponseHandler only download one file at a time.
 *   - FileAsyncHttpResponseHandler interface to downloading a file is more convenient than verbose use of
 *     DefaultHttpClient within an async task.
 *   - Stack depth is expected to be quite small (in the 100-s).
 */
class DictDownloader {
    private static final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

    private final GetDictionariesActivity getDictionariesActivity;
    private final List<Boolean> dictFailure;
    private final List<String> downloadedArchiveBasenames;
    private final ArrayList<String> dictionariesSelectedLst;
    private final File downloadsDir;
    private final ProgressBar progressBar;
    private final TextView topText;

    DictDownloader(GetDictionariesActivity getDictionariesActivity, List<Boolean> dictFailure,
                          List<String> downloadedArchiveBasenames, ArrayList<String> dictionariesSelectedLst, File downloadsDir,
                          ProgressBar progressBar, TextView topText) {
        this.getDictionariesActivity = getDictionariesActivity;
        this.dictFailure = dictFailure;
        this.downloadedArchiveBasenames = downloadedArchiveBasenames;
        this.dictionariesSelectedLst = dictionariesSelectedLst;
        this.downloadsDir = downloadsDir;
        this.topText = topText;
        this.progressBar = progressBar;
    }

    // Runs in the UI thread.
    // Log errors, record failure, get the next dictionary.
    private void handleDownloadDictFailure(final int index, String url, Throwable throwable) {
        String message = "Failed to get " + url;
        Log.e("downloadDict", message + ":", throwable);
        topText.setText(message);
        dictFailure.set(index, true);
        downloadDict(index + 1);
        progressBar.setVisibility(View.GONE);
    }

    void downloadDict(final int index) {
        // Stop condition of the recursion.
        if (index >= dictionariesSelectedLst.size()) {
            getDictionariesActivity.whenAllDictsDownloaded();
            return;
        }
        MainActivity.getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getDictionariesActivity);

        topText.setText(String.format(getDictionariesActivity.getString(R.string.gettingSomeDict), dictionariesSelectedLst.get(index)));
        topText.append("\n" + getDictionariesActivity.getString(R.string.dont_navigate_away));
        Log.d("downloadDict ", topText.getText().toString());
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
                    downloadedArchiveBasenames.add(fileName);
                    Log.i("Got dictionary: ", fileName);
                    dictFailure.set(index, false);
                    downloadDict(index + 1);
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    super.onProgress(bytesWritten, totalSize);
                    getDictionariesActivity.updateProgressBar((int) bytesWritten, (int) totalSize);
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

}
