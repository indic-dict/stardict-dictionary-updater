package sanskritCode.downloaderFlow

import android.Manifest
import android.annotation.SuppressLint
import android.os.AsyncTask
import android.util.Log

import com.google.common.io.Files

import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import sanskritCode.downloaderFlow.DictIndexStore
import sanskritCode.downloaderFlow.DictInfo
import sanskritCode.downloaderFlow.DictStatus

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 * Extracts all selected dictionaries sequentially in ONE Async task (doBackground() being run outside the UI thread).
 */
internal class DictExtractor(@field:SuppressLint("StaticFieldLeak")
                             private val activity: ExtractDictionariesActivity, private val dictDir: File, private val dictIndexStore: DictIndexStore, private val downloadsDir: File) : AsyncTask<Void, String, Void?> /* params, progress, result */() {
    private val compressorStreamFactory = CompressorStreamFactory(true /*equivalent to setDecompressConcatenated*/)
    private val archiveStreamFactory = ArchiveStreamFactory()
    private val LOGGER_TAG = javaClass.getSimpleName()
    // See http://stardict-4.sourceforge.net/StarDictFileFormat
    private val VALID_NON_RESOURCE_FILE_EXTENSIONS_REGEX = "ifo|dz|dict|idx|syn|rifo|ridx|rdic"

    private fun deleteTarFile(sourceFile: String) {
        val message4 = "Deleting " + sourceFile + " " + File(sourceFile).delete()
        // topText.append(message4);
        Log.d(LOGGER_TAG, ":handleDownloadDictFailure:$message4")
    }

    override fun onPostExecute(result: Void?) {
        activity.whenAllDictsExtracted()
    }

    private fun cleanDirectory(directory: File) {
        Log.i(LOGGER_TAG, ":cleanDirectory:Cleaning $directory")

        val files = directory.listFiles()
        Log.d(LOGGER_TAG, ":cleanDirectory:" + "Deleting " + files!!.size)

        if (files == null) {  // null if security restricted
            Log.e(LOGGER_TAG, ":cleanDirectory:" + "Could not list files - got null")
        }

        for (file in files) {
            if (file.isDirectory) {
                cleanDirectory(file)
            }
            Log.d(LOGGER_TAG, ":cleanDirectory:" + "Deleting " + file.name + " with result " + file.delete())
        }
    }

    @Throws(FileNotFoundException::class, CompressorException::class, ArchiveException::class)
    private fun inputStreamFromArchive(sourceFile: String): ArchiveInputStream {
        // To handle "IllegalArgumentException: Mark is not supported", we wrap with a BufferedInputStream
        // as suggested in http://apache-commons.680414.n4.nabble.com/Compress-Reading-archives-within-archives-td746866.html
        return archiveStreamFactory.createArchiveInputStream(
                BufferedInputStream(compressorStreamFactory.createCompressorInputStream(
                        BufferedInputStream(FileInputStream(sourceFile))
                )))
    }

    override fun onProgressUpdate(vararg values: String) {
        super.onProgressUpdate(*values)
        activity.updateProgressBar(Integer.parseInt(values[0]), Integer.parseInt(values[1]))
        activity.setTopTextWhileExtracting(values[2], values[3])
    }

    private fun extractFile(dictInfo: DictInfo) {
        val archiveFileName = dictInfo.downloadedArchiveBasename
        if (dictInfo.status != DictStatus.DOWNLOAD_SUCCESS) {
            Log.w(LOGGER_TAG, ":extractFile:" + "Skipping " + archiveFileName + " with status " + dictInfo.status)
            return
        }

        val sourceFile = File(downloadsDir.toString(), archiveFileName).absolutePath
        activity.getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, activity)
        publishProgress(Integer.toString(0), Integer.toString(1), archiveFileName, "")

        // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27
        val baseName = Files.getNameWithoutExtension(Files.getNameWithoutExtension(archiveFileName!!)).split("__".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
        val destDirFile = File(dictDir.toString(), baseName)
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
            var baseNameAccordingToArchiveEntries: String? = null
            val resourceDirFile = File(initialDestDir, "res")
            var filesRead = 0
            while (true) {
                var currentEntry = archiveInputStream.nextEntry
                if (currentEntry == null) {
                    break
                }
                filesRead = filesRead + 1
                val destFileName = String.format("%s.%s", Files.getNameWithoutExtension(currentEntry.name), Files.getFileExtension(currentEntry.name))
                val destFileExtension = Files.getFileExtension(destFileName)
                val isResourceFile = !destFileName.isEmpty() && currentEntry.name.replace(destFileName, "").contains("/res/")
                var destFileDir = initialDestDir
                if (isResourceFile) {
                    destFileDir = resourceDirFile.absolutePath + currentEntry.name.replace(destFileName, "").replaceFirst(".*/res/".toRegex(), "")
                }

                if (isResourceFile || destFileExtension.matches(VALID_NON_RESOURCE_FILE_EXTENSIONS_REGEX.toRegex())) {
                    val destFile = File(destFileDir, destFileName).absolutePath
                    val destFileDirFile = File(destFileDir)
                    if (!isResourceFile && destFileExtension.matches(VALID_NON_RESOURCE_FILE_EXTENSIONS_REGEX.toRegex())) {
                        val baseNameAccordingToArchiveEntry = DictNameHelper.getNameWithoutAnyExtension(destFileName)
                        if (baseNameAccordingToArchiveEntries == null) {
                            baseNameAccordingToArchiveEntries = baseNameAccordingToArchiveEntry
                        } else {
                            if (baseNameAccordingToArchiveEntries != baseNameAccordingToArchiveEntry) {
                                throw Exception("baseNameAccordingToArchiveEntries inconsistent for: $destFileName  Expected $baseNameAccordingToArchiveEntries")
                            }
                        }
                    }
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
                } else {
                    Log.w(LOGGER_TAG, ":extractFile:" + "Not extracting " + currentEntry.name)

                    Log.d(LOGGER_TAG, ":extractFile:isResourceFile $isResourceFile")
                    val message3 = "Destination: " + destFileName + "\nArchive entry: " + currentEntry.name
                    Log.d(LOGGER_TAG, ":extractFile:$message3")
                }
            }
            archiveInputStream.close()
            if (baseNameAccordingToArchiveEntries != null && baseNameAccordingToArchiveEntries != baseName) {
                Log.d(LOGGER_TAG, ":extractFile:baseName: $baseName, baseNameAccordingToArchiveEntries: $baseNameAccordingToArchiveEntries")
                val finalDestDir = File(dictDir.toString(), baseNameAccordingToArchiveEntries).absolutePath
                Log.d(LOGGER_TAG, ":extractFile:" + "destDirFile: " + destDirFile.toString() + ", finalDestDir: " + finalDestDir)
                val finalDestDirFile = File(finalDestDir)
                if (finalDestDirFile.exists()) {
                    cleanDirectory(finalDestDirFile)
                    Log.w(LOGGER_TAG, ":extractFile:" + "Deleting preexisting dict directory with result: " + finalDestDirFile.delete())
                }
                val result = destDirFile.renameTo(finalDestDirFile)
                Log.w(LOGGER_TAG, ":extractFile:Renaming the dict directory with result: $result")
            }


            dictInfo.status = DictStatus.EXTRACTION_SUCCESS
            Log.d(LOGGER_TAG, ":extractFile:" + "success!")
            activity.storeDictVersion(archiveFileName)
        } catch (e: Exception) {
            Log.e(LOGGER_TAG, ":extractFile:" + "IOEx:", e)
            dictInfo.status = DictStatus.EXTRACTION_FAILURE
        }

        deleteTarFile(sourceFile)

    }

    override fun doInBackground(vararg params: Void): Void? {
        for (dictInfo in dictIndexStore.dictionariesSelectedMap.values) {
            extractFile(dictInfo)
        }
        return null
    }
}
