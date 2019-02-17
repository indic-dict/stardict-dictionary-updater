package sanskritCode.downloaderFlow

import android.Manifest
import android.annotation.SuppressLint
import android.os.AsyncTask
import android.util.Log

import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 * Extracts all selected archives sequentially in ONE Async task (doBackground() being run outside the UI thread).
 */
internal class ArchiveExtractor(@field:SuppressLint("StaticFieldLeak")
                             private val activity: ExtractArchivesActivity, private val archiveDir: File, private val archiveIndexStore: ArchiveIndexStore, private val downloadsDir: File) : AsyncTask<Void, String, Void?> /* params, progress, result */() {
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

    private fun cleanDirectory(directory: File) {
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

        val sourceFile = File(downloadsDir.toString(), archiveFileName).absolutePath
        activity.getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, activity)
        publishProgress(Integer.toString(0), Integer.toString(1), archiveFileName, "")

        val baseName = archiveInfo.getUnversionedArchiveBaseName()
        val destDirFile = File(archiveDir.toString(), baseName)
        val initialDestDir = destDirFile.absolutePath

        Log.d(LOGGER_TAG, ":extractFile:" + "Exists " + destDirFile.exists() + " isDir " + destDirFile.isDirectory)
        if (destDirFile.exists()) {
            Log.i(LOGGER_TAG, ":extractFile:Cleaning $initialDestDir")
            cleanDirectory(destDirFile)
        } else {
            Log.i(LOGGER_TAG, ":extractFile:" + "Creating afresh the directory " + destDirFile.mkdirs())
        }
        Log.d(LOGGER_TAG, ":extractFile:" + "Exists " + destDirFile.exists() + " isDir " + destDirFile.isDirectory)

        val message2 = "Destination directory $initialDestDir"
        Log.d(LOGGER_TAG, ":extractFile:$message2")
        try {
            var archiveInputStream = inputStreamFromArchive(sourceFile)
            var totalFiles = 0
            while (archiveInputStream.nextEntry != null) {
                totalFiles = totalFiles + 1
            }
            if (totalFiles == 0) {
                throw Exception("0 files in archive??!")
            }

            // Reopen stream
            archiveInputStream = inputStreamFromArchive(sourceFile)

            val buffer = ByteArray(50000)
            val baseNameAccordingToArchiveEntries: String? = null
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
                val destFileDirFile = File(initialDestDir, File(currentEntry.name).parent?:"")
                val destFile = File(destFileDirFile, File(currentEntry.name).name).absolutePath
                if (!destFileDirFile.exists()) {
                    Log.i(LOGGER_TAG, ":extractFile:" + "Creating afresh the directory " + destFileDirFile + ", result:" + destFileDirFile.mkdirs())
                }
                val fos = FileOutputStream(destFile)
                var n: Int
                while (true) {
                    n = archiveInputStream.read(buffer)
                    if (n == -1) {
                        break
                    }
                    fos.write(buffer, 0, n)
                }
                fos.close()
                publishProgress(Integer.toString(filesRead), Integer.toString(totalFiles), archiveFileName, destFile)
            }
            archiveInputStream.close()
            if (baseNameAccordingToArchiveEntries != null && baseNameAccordingToArchiveEntries != baseName) {
                Log.d(LOGGER_TAG, ":extractFile:baseName: $baseName, baseNameAccordingToArchiveEntries: $baseNameAccordingToArchiveEntries")
                val finalDestDir = File(archiveDir.toString(), baseNameAccordingToArchiveEntries).absolutePath
                Log.d(LOGGER_TAG, ":extractFile:" + "destDirFile: " + destDirFile.toString() + ", finalDestDir: " + finalDestDir)
                val finalDestDirFile = File(finalDestDir)
                if (finalDestDirFile.exists()) {
                    cleanDirectory(finalDestDirFile)
                    Log.w(LOGGER_TAG, ":extractFile:" + "Deleting preexisting destination directory with result: " + finalDestDirFile.delete())
                }
                val result = destDirFile.renameTo(finalDestDirFile)
                Log.w(LOGGER_TAG, ":extractFile:Renaming the destination directory with result: $result")
            }


            archiveInfo.status = ArchiveStatus.EXTRACTION_SUCCESS
            Log.d(LOGGER_TAG, ":extractFile:" + "success!")
            activity.storeArchiveVersion(archiveFileName)
        } catch (e: Exception) {
            Log.e(LOGGER_TAG, ":extractFile:" + "IOEx:", e)
            archiveInfo.status = ArchiveStatus.EXTRACTION_FAILURE
        }

        deleteTarFile(sourceFile)

    }

    override fun doInBackground(vararg params: Void): Void? {
        for (archiveInfo in archiveIndexStore.archivesSelectedMap.values) {
            extractFile(archiveInfo)
        }
        return null
    }
}
