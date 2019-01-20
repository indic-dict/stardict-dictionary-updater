package sanskritcode.sanskritdictionaryupdater

import android.Manifest
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.FileAsyncHttpResponseHandler

import java.io.File

/**
 * Expected flow: downloadDict(0) -> ... -> downloadDict(index) ->  downloadDict(index + 1) -> ...
 *
 * Why are we using recursion?
 * - FileAsyncHttpResponseHandler only download one file at a time.
 * - FileAsyncHttpResponseHandler interface to downloading a file is more convenient than verbose use of
 * DefaultHttpClient within an async task.
 * - Stack depth is expected to be quite small (in the 100-s).
 */
internal class DictDownloader(private val getDictionariesActivity: GetDictionariesActivity, private val dictIndexStore: DictIndexStore, private val downloadsDir: File, private val progressBar: ProgressBar, private val topText: TextView) {
    private val LOGGER_TAG = javaClass.getSimpleName()

    // Runs in the UI thread.
    // Log errors, record failure, get the next dictionary.
    private fun handleDownloadDictFailure(index: Int, url: String, throwable: Throwable) {
        val message = "Failed to get $url"
        Log.e(LOGGER_TAG, ":handleDownloadDictFailure:$message:", throwable)
        topText.text = message
        dictIndexStore.dictionariesSelectedMap[url]!!.status = DictStatus.DOWNLOAD_FAILURE
        downloadDict(index + 1)
        progressBar.visibility = View.GONE
    }

    fun downloadDict(index: Int) {
        // Stop condition of the recursion.
        if (index >= dictIndexStore.dictionariesSelectedMap.size) {
            getDictionariesActivity.whenAllDictsDownloaded()
            return
        }
        val dictInfo = dictIndexStore.dictionariesSelectedMap.values.toTypedArray()[index]
        val url = dictInfo.url
        // TODO: Getting messages like:  Skipping null withs status DOWNLOAD_FAILURE .
        if (dictInfo.status != DictStatus.NOT_TRIED) {
            Log.w(LOGGER_TAG, ":downloadDict:" + "Skipping " + url + " with status " + dictInfo.status)
            downloadDict(index + 1)
            return
        }
        Log.d(LOGGER_TAG, ":downloadDict:Getting $url")
        getDictionariesActivity.getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getDictionariesActivity)

        topText.setText(String.format(getDictionariesActivity.getString(R.string.gettingSomeDict), url))
        topText.append("\n" + getDictionariesActivity.getString(R.string.dont_navigate_away))
        Log.d(LOGGER_TAG, "downloadDict: " + topText.text.toString())
        try {
            val fileName = url.substring(url.lastIndexOf("/")).replace("/", "")
            if (fileName.isEmpty()) {
                throw IllegalArgumentException("fileName is empty for url $url")
            }
            asyncHttpClient.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:32.0) Gecko/20100101 Firefox/32.0")
            asyncHttpClient.setEnableRedirects(true, true, true)
            // URL could be bad, hence the below.
            asyncHttpClient.get(url, object : FileAsyncHttpResponseHandler(File(downloadsDir, fileName)) {
                override fun onSuccess(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, file: File) {
                    dictIndexStore.dictionariesSelectedMap[url]!!.downloadedArchiveBasename = fileName
                    Log.i(LOGGER_TAG, ":downloadDict:Got dictionary: $fileName")
                    dictIndexStore.dictionariesSelectedMap[url]!!.status = DictStatus.DOWNLOAD_SUCCESS
                    downloadDict(index + 1)
                    progressBar.visibility = View.GONE
                }

                override fun onProgress(bytesWritten: Long, totalSize: Long) {
                    super.onProgress(bytesWritten, totalSize)
                    getDictionariesActivity.updateProgressBar(bytesWritten.toInt(), totalSize.toInt())
                }

                override fun onFailure(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, throwable: Throwable, file: File) {
                    Log.e(LOGGER_TAG, ":onFailure:status $statusCode")
                    handleDownloadDictFailure(index, url, throwable)
                }
            })
        } catch (throwable: Throwable) {
            handleDownloadDictFailure(index, url, throwable)
        }

    }

    companion object {
        private val asyncHttpClient = AsyncHttpClient()
    }

}
