package sanskritCode.downloaderFlow

import android.Manifest
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.FileAsyncHttpResponseHandler

import java.io.File

/**
 * Expected flow: downloadArchive(0) -> ... -> downloadArchive(index) ->  downloadArchive(index + 1) -> ...
 *
 * Why are we using recursion?
 * - FileAsyncHttpResponseHandler only download one file at a time.
 * - FileAsyncHttpResponseHandler interface to downloading a file is more convenient than verbose use of
 * DefaultHttpClient within an async task.
 * - Stack depth is expected to be quite small (in the 100-s).
 */
internal class ArchiveDownloader(private val getArchivesActivity: GetArchivesActivity, private val archiveIndexStore: ArchiveIndexStore, private val downloadsDir: File, private val progressBar: ProgressBar, private val topText: TextView) {
    private val LOGGER_TAG = javaClass.getSimpleName()

    // Runs in the UI thread.
    // Log errors, record failure, get the next archive.
    private fun handleDownloadFailure(index: Int, url: String, throwable: Throwable) {
        val message = "Failed to get $url"
        Log.e(LOGGER_TAG, ":handleDownloadFailure:$message:", throwable)
        topText.text = message
        archiveIndexStore.archivesSelectedMap[url]!!.status = ArchiveStatus.DOWNLOAD_FAILURE
        downloadArchive(index + 1)
        progressBar.visibility = View.GONE
    }

    fun downloadArchive(index: Int) {
        // Stop condition of the recursion.
        if (index >= archiveIndexStore.archivesSelectedMap.size) {
            getArchivesActivity.whenAllDownloaded()
            return
        }
        val archiveInfo = archiveIndexStore.archivesSelectedMap.values.toTypedArray()[index]
        val url = archiveInfo.url
        // TODO: Getting messages like:  Skipping null withs status DOWNLOAD_FAILURE .
        if (archiveInfo.status != ArchiveStatus.NOT_TRIED) {
            Log.w(LOGGER_TAG, ":downloadArchive:" + "Skipping " + url + " with status " + archiveInfo.status)
            downloadArchive(index + 1)
            return
        }
        Log.d(LOGGER_TAG, ":downloadArchive:Getting $url")
        getArchivesActivity.getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getArchivesActivity)

        topText.setText(String.format(getArchivesActivity.getString(R.string.df_gettingSomeArchive), url))
        topText.append("\n" + getArchivesActivity.getString(R.string.dont_navigate_away))
        Log.d(LOGGER_TAG, "downloadArchive: " + topText.text.toString())
        try {
            val fileName = archiveInfo.getDownloadedArchiveBasename()
            if (fileName.isEmpty()) {
                throw IllegalArgumentException("fileName is empty for url $url")
            }
            asyncHttpClient.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:32.0) Gecko/20100101 Firefox/32.0")
            asyncHttpClient.setEnableRedirects(true, true, true)
            // URL could be bad, hence the below.
            asyncHttpClient.get(url, object : FileAsyncHttpResponseHandler(File(downloadsDir, fileName)) {
                override fun onSuccess(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, file: File) {
                    Log.i(LOGGER_TAG, ":downloadArchive:Got archive: $fileName")
                    archiveIndexStore.archivesSelectedMap[url]!!.status = ArchiveStatus.DOWNLOAD_SUCCESS
                    downloadArchive(index + 1)
                    progressBar.visibility = View.GONE
                }

                override fun onProgress(bytesWritten: Long, totalSize: Long) {
                    super.onProgress(bytesWritten, totalSize)
                    getArchivesActivity.updateProgressBar(bytesWritten.toInt(), totalSize.toInt())
                }

                override fun onFailure(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, throwable: Throwable, file: File) {
                    Log.e(LOGGER_TAG, ":onFailure:status $statusCode")
                    handleDownloadFailure(index, url, throwable)
                }
            })
        } catch (throwable: Throwable) {
            handleDownloadFailure(index, url, throwable)
        }

    }

    companion object {
        private val asyncHttpClient = AsyncHttpClient()
    }

}
