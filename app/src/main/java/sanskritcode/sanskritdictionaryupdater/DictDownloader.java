package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;

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
    private final File downloadsDir;
    private final ProgressBar progressBar;
    private final TextView topText;
    private final DictIndexStore dictIndexStore;


    DictDownloader(GetDictionariesActivity getDictionariesActivity, DictIndexStore dictIndexStore, File downloadsDir, ProgressBar progressBar, TextView topText) {
        this.getDictionariesActivity = getDictionariesActivity;
        this.dictIndexStore = dictIndexStore;
        this.downloadsDir = downloadsDir;
        this.topText = topText;
        this.progressBar = progressBar;
    }

    // Runs in the UI thread.
    // Log errors, record failure, get the next dictionary.
    private void handleDownloadDictFailure(final int index, String url, Throwable throwable) {
        String message = "Failed to get " + url;
        String LOGGER_NAME = (getClass().getSimpleName() + ":handleDownloadDictFailure").substring(0,26);
        Log.e(LOGGER_NAME, message + ":", throwable);
        topText.setText(message);
        dictIndexStore.dictionariesSelectedMap.get(url).status = DictStatus.DOWNLOAD_FAILURE;
        downloadDict(index + 1);
        progressBar.setVisibility(View.GONE);
    }

    void downloadDict(final int index) {
        String LOGGER_NAME = getClass().getSimpleName() + ":downloadDict";
        // Stop condition of the recursion.
        if (index >= dictIndexStore.dictionariesSelectedMap.size()) {
            getDictionariesActivity.whenAllDictsDownloaded();
            return;
        }
        final DictInfo dictInfo = (DictInfo) dictIndexStore.dictionariesSelectedMap.values().toArray()[index];
        final String url = dictInfo.url;
        // TODO: Getting messages like:  Skipping null withs status DOWNLOAD_FAILURE .
        if (dictInfo.status != DictStatus.NOT_TRIED) {
            Log.w(LOGGER_NAME, "Skipping " + url + " with status " + dictInfo.status);
            downloadDict(index + 1);
            return;
        }
        Log.d(LOGGER_NAME, "Getting " + url);
        MainActivity.getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getDictionariesActivity);

        topText.setText(String.format(getDictionariesActivity.getString(R.string.gettingSomeDict), url));
        topText.append("\n" + getDictionariesActivity.getString(R.string.dont_navigate_away));
        Log.d("downloadDict ", topText.getText().toString());
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
                    dictIndexStore.dictionariesSelectedMap.get(url).downloadedArchiveBasename = fileName;
                    Log.i("Got dictionary: ", fileName);
                    dictIndexStore.dictionariesSelectedMap.get(url).status = DictStatus.DOWNLOAD_SUCCESS;
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
                    final String LOGGER_NAME = (getClass().getSimpleName() + ":onFailure").substring(0,26);
                    Log.e(LOGGER_NAME, "status " + statusCode);
                    handleDownloadDictFailure(index, url, throwable);
                }
            });
        } catch (Throwable throwable) {
            handleDownloadDictFailure(index, url, throwable);
        }
    }

}
