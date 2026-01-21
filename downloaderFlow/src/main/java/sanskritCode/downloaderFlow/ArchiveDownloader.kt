package sanskritCode.downloaderFlow

import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.FileAsyncHttpResponseHandler
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.BufferedInputStream

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

/**
 * Expected flow: downloadArchive(0) -> ... -> downloadArchive(index) ->  downloadArchive(index + 1) -> ...
 *
 * Why are we using recursion?
 * - FileAsyncHttpResponseHandler only download one file at a time.
 * - FileAsyncHttpResponseHandler interface to downloading a file is more convenient than verbose use of
 * DefaultHttpClient within an async task.
 * - Stack depth is expected to be quite small (in the 100-s).
 */
internal class ArchiveDownloader(
    private val getArchivesActivity: GetArchivesActivity,
    private val archiveIndexStore: ArchiveIndexStore,
    private val downloadsDir: File,
    private val destDir: DocumentFile,
    private val criticalFilesPattern: Regex,
    private val progressBar: ProgressBar,
    private val topText: TextView,
    private val sharedArchiveInfoStore: SharedPreferences
) {
    private val LOGGER_TAG = javaClass.getSimpleName()
    private val compressorStreamFactory = CompressorStreamFactory(true /*equivalent to setDecompressConcatenated*/)
    private val archiveStreamFactory = ArchiveStreamFactory()

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
            asyncHttpClient.setLoggingLevel(Log.INFO)

            // URL could be bad, hence the below.
            asyncHttpClient.get(url, object : FileAsyncHttpResponseHandler(File(downloadsDir, fileName)) {
                override fun onSuccess(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, file: File) {
                    Log.i(LOGGER_TAG, ":downloadArchive:Got archive: ${file.canonicalPath}")
                    val archiveInfo = archiveIndexStore.archivesSelectedMap[url]
                    archiveInfo!!.archivePath = file.canonicalPath
                    archiveInfo!!.status = ArchiveStatus.DOWNLOAD_SUCCESS
                    extractFile(archiveInfo, destDir, criticalFilesPattern)
                    downloadArchive(index + 1)
                    progressBar.visibility = View.GONE
                }

                override fun onProgress(bytesWritten: Long, totalSize: Long) {
                    super.onProgress(bytesWritten, totalSize)
                    getArchivesActivity.updateProgressBar(bytesWritten.toInt(), totalSize.toInt())
                }

                override fun onFailure(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>?, throwable: Throwable, file: File) {
                    Log.e(LOGGER_TAG, ":onFailure:status ${statusCode}")
                    handleDownloadFailure(index, url, throwable)
                }
            })
        } catch (throwable: Throwable) {
            Log.w(LOGGER_TAG, downloadsDir.absolutePath)
            handleDownloadFailure(index, url, throwable)
        }

    }

    private fun createDir(baseDir: DocumentFile, dirPath: String): DocumentFile? {
        var destDirFile = baseDir.findFile(dirPath)
        if (destDirFile == null) {
            destDirFile = baseDir.createDirectory(dirPath)
        }
        return destDirFile
    }


    private fun createFile(baseDir: DocumentFile, entry: ArchiveEntry): DocumentFile? {
        var baseDirLocal = baseDir
        if (File(entry.name).parent != null) {
            val folders: List<String> = File(entry.name).parent.toString().split("/")
            for (folder in folders) {
                baseDirLocal = createDir(baseDirLocal, folder)!!
            }
        }
        var destFile = baseDirLocal.findFile(File(entry.name).name)
        if (destFile == null) {
            destFile = baseDirLocal.createFile("application/octet-stream", File(entry.name).name)
        }
        return destFile
    }


    private fun cleanDirectory(directory: DocumentFile) {
        Log.i(LOGGER_TAG, ":cleanDirectory:Cleaning $directory")

        val files = directory.listFiles()
        Log.d(LOGGER_TAG, ":cleanDirectory:" + "Deleting " + files!!.size)

        for (file in files) {
            if (file.isDirectory) {
                cleanDirectory(file)
            }
            Log.d(LOGGER_TAG, ":cleanDirectory:" + "Deleting " + file.name + " with result " + file.delete())
        }
    }


    @Throws(FileNotFoundException::class, CompressorException::class, ArchiveException::class)
    private fun inputStreamFromArchive(sourceFile: String): ArchiveInputStream {
        try {
            // To handle "IllegalArgumentException: Mark is not supported", we wrap with a BufferedInputStream
            // as suggested in http://apache-commons.680414.n4.nabble.com/Compress-Reading-archives-within-archives-td746866.html
            return archiveStreamFactory.createArchiveInputStream(
                BufferedInputStream(compressorStreamFactory.createCompressorInputStream(
                    BufferedInputStream(FileInputStream(sourceFile))
                ))
            )
        } catch (compressorException: CompressorException) {
            return archiveStreamFactory.createArchiveInputStream(
                BufferedInputStream(FileInputStream(sourceFile))
            )
        }
    }

    private fun deleteTarFile(sourceFile: String) {
        val message4 = "Deleting " + sourceFile + " " + File(sourceFile).delete()
        // topText.append(message4);
        Log.d(LOGGER_TAG, ":handleDownloadArchiveFailure:$message4")
    }

    private fun extractFile(archiveInfo: ArchiveInfo, destDir: DocumentFile, criticalFilesPattern: Regex) {
        val archiveFileName = archiveInfo.getDownloadedArchiveBasename()
        if (archiveInfo.status != ArchiveStatus.DOWNLOAD_SUCCESS) {
            Log.w(LOGGER_TAG, ":extractFile:" + "Skipping " + archiveFileName + " with status " + archiveInfo.status)
            return
        }

        val sourceFile = File(archiveInfo.archivePath)
        if (!sourceFile.exists()) {
            archiveInfo.status = ArchiveStatus.ARCHIVE_FILE_MISSING
            Log.w(LOGGER_TAG, ":extractFile:" + "Skipping " + archiveFileName + " with status "  + archiveInfo.status)
            return
        }
        getArchivesActivity.setTopTextWhileExtracting(archiveFileName, "")
        getArchivesActivity.updateProgressBar(0, 1)

        var destDirFile = createDir(destDir, archiveInfo.getDestinationPathSuffix())

        Log.d(LOGGER_TAG, ":extractFile:" + "Exists " + destDirFile?.exists() + " isDir " + destDirFile?.isDirectory)
        Log.i(LOGGER_TAG, ":extractFile:Cleaning ${destDirFile?.uri}")
        cleanDirectory(destDirFile!!)
        Log.d(LOGGER_TAG, ":extractFile:" + "Exists " + destDirFile?.exists() + " isDir " + destDirFile?.isDirectory)

        val message2 = "Destination directory ${destDirFile?.uri}"
        Log.d(LOGGER_TAG, ":extractFile:$message2")
        try {
            var archiveInputStream = inputStreamFromArchive(sourceFile.absolutePath)
            var totalFiles = 0
            while (archiveInputStream.nextEntry != null) {
                totalFiles = totalFiles + 1
            }
            if (totalFiles == 0) {
                throw Exception("0 files in archive??!")
            }

            // Reopen stream
            archiveInputStream = inputStreamFromArchive(sourceFile.absolutePath)

            val buffer = ByteArray(50000)
            var filesRead = 0
            while (true) {
                val currentEntry = archiveInputStream.nextEntry
                if (currentEntry == null) {
                    break
                }
                filesRead = filesRead + 1
                if(currentEntry.isDirectory) {
                    continue
                }
                // TODO: Make this optional. Skipping non-essential files for speed.
                if(!currentEntry.name.matches(criticalFilesPattern)) {
                    continue
                }
                getArchivesActivity.setTopTextWhileExtracting(archiveFileName, currentEntry.name)
                destDirFile?.uri
                val destFile = createFile(destDirFile!!, currentEntry)
                val fos = getArchivesActivity.getContentResolver().openOutputStream(destFile?.getUri()!!)!!
                var n: Int
                while (true) {
                    n = archiveInputStream.read(buffer)
                    if (n == -1) {
                        break
                    }
                    fos.write(buffer, 0, n)
                }
                fos.close()
                getArchivesActivity.updateProgressBar(filesRead, totalFiles)
            }
            archiveInputStream.close()
            archiveInfo.status = ArchiveStatus.EXTRACTION_SUCCESS
            val editor = sharedArchiveInfoStore.edit()
            archiveInfo.storeToSharedPreferences(editor)
            Log.d(LOGGER_TAG, ":extractFile:" + "success!")
        } catch (e: Exception) {
            Log.e(LOGGER_TAG, ":extractFile:" + "IOEx:", e)
            archiveInfo.status = ArchiveStatus.EXTRACTION_FAILURE
        }

        deleteTarFile(sourceFile.absolutePath)

    }

    companion object {
        private val asyncHttpClient = AsyncHttpClient()
    }

}
