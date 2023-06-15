package sanskritCode.downloaderFlow

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.*

/**
 * Extracts all selected archives sequentially in ONE Async task (doBackground() being run outside the UI thread).
 */
internal class ArchiveExtractor(@field:SuppressLint("StaticFieldLeak")
                             private val activity: ExtractArchivesActivity, private val destDir: DocumentFile, private val archiveIndexStore: ArchiveIndexStore, private val criticalFilesPattern: Regex) : AsyncTask<Void, String, Void?> /* params, progress, result */() {
    private val compressorStreamFactory = CompressorStreamFactory(true /*equivalent to setDecompressConcatenated*/)
    private val archiveStreamFactory = ArchiveStreamFactory()
    private val LOGGER_TAG = javaClass.getSimpleName()

    private fun deleteTarFile(sourceFile: String) {
        val message4 = "Deleting " + sourceFile + " " + File(sourceFile).delete()
        // topText.append(message4);
        Log.d(LOGGER_TAG, ":handleDownloadArchiveFailure:$message4")
    }

    override fun onPostExecute(result: Void?) {
        activity.whenAllExtracted()
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
                    )))
        } catch (compressorException: CompressorException) {
            return archiveStreamFactory.createArchiveInputStream(
                   BufferedInputStream(FileInputStream(sourceFile)))
        }
    }

    override fun onProgressUpdate(vararg values: String) {
        super.onProgressUpdate(*values)
        activity.updateProgressBar(Integer.parseInt(values[0]), Integer.parseInt(values[1]))
        activity.setTopTextWhileExtracting(values[2], values[3])
    }

    private fun extractFile(archiveInfo: ArchiveInfo) {
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
        publishProgress(Integer.toString(0), Integer.toString(1), archiveFileName, "")

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
                destDirFile?.uri
                val destFile = createFile(destDirFile!!, currentEntry)
                val fos = activity.getContentResolver().openOutputStream(destFile?.getUri()!!)!!
                var n: Int
                while (true) {
                    n = archiveInputStream.read(buffer)
                    if (n == -1) {
                        break
                    }
                    fos.write(buffer, 0, n)
                }
                fos.close()
                publishProgress(Integer.toString(filesRead), Integer.toString(totalFiles), archiveFileName, destFile.uri.toString())
            }
            archiveInputStream.close()
            archiveInfo.status = ArchiveStatus.EXTRACTION_SUCCESS
            Log.d(LOGGER_TAG, ":extractFile:" + "success!")
        } catch (e: Exception) {
            Log.e(LOGGER_TAG, ":extractFile:" + "IOEx:", e)
            archiveInfo.status = ArchiveStatus.EXTRACTION_FAILURE
        }

        deleteTarFile(sourceFile.absolutePath)

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

    override fun doInBackground(vararg params: Void): Void? {
        for (archiveInfo in archiveIndexStore.archivesSelectedMap.values.distinct()) {
            extractFile(archiveInfo)
        }
        return null
    }
}
