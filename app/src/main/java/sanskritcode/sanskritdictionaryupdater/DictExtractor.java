package sanskritcode.sanskritdictionaryupdater;

import android.Manifest;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.io.Files;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Extracts all selected dictionaries sequentially in ONE Async task (doBackground() being run outside the UI thread).
 */
class DictExtractor extends AsyncTask<Void, String, Void> /* params, progress, result */ {
    private final CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory(true /*equivalent to setDecompressConcatenated*/);
    private final ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
    private final ExtractDictionariesActivity activity;
    private final File downloadsDir;
    // See http://stardict-4.sourceforge.net/StarDictFileFormat
    private final String VALID_NON_RESOURCE_FILE_EXTENSIONS_REGEX = "ifo|dz|dict|idx|syn|rifo|ridx|rdic";
    private final DictIndexStore dictIndexStore;
    private final File dictDir;

    DictExtractor(ExtractDictionariesActivity activity, File dictDir, DictIndexStore dictIndexStore, File downloadsDir) {
        this.activity = activity;
        this.dictIndexStore = dictIndexStore;
        this.dictDir = dictDir;
        this.downloadsDir = downloadsDir;
    }

    private void deleteTarFile(String sourceFile) {
        String LOGGER_NAME = (getClass().getSimpleName() + ":handleDownloadDictFailure").substring(0,26);
        String message4 = "Deleting " + sourceFile + " " + new File(sourceFile).delete();
        // topText.append(message4);
        Log.d(LOGGER_NAME, message4);
    }

    @Override
    protected void onPostExecute(Void result) {
        activity.whenAllDictsExtracted();
    }

    private void cleanDirectory(File directory) {
        String LOGGER_NAME = getClass().getSimpleName() + ":cleanDirectory";
        Log.i(LOGGER_NAME, "Cleaning " + directory);

        File[] files = directory.listFiles();
        Log.d(LOGGER_NAME, "Deleting " + files.length);
        //noinspection ConstantConditions
        if (files == null) {  // null if security restricted
            Log.e(LOGGER_NAME, "Could not list files - got null");
        }

        for (File file : files) {
            if (file.isDirectory()) {
                cleanDirectory(file);
            }
            Log.d(LOGGER_NAME, "Deleting " + file.getName() + " with result " + file.delete());
        }
    }

    private ArchiveInputStream inputStreamFromArchive(String sourceFile) throws FileNotFoundException, CompressorException, ArchiveException {
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
        activity.updateProgressBar(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
        activity.setTopTextWhileExtracting(values[2], values[3]);
    }

    private void extractFile(DictInfo dictInfo) {
        String archiveFileName = dictInfo.downloadedArchiveBasename;
        String LOGGER_NAME = getClass().getSimpleName() + ":extractFile";
        if ( dictInfo.status != DictStatus.DOWNLOAD_SUCCESS) {
            Log.w(LOGGER_NAME, "Skipping " + archiveFileName + " withs status " + dictInfo.status);
            return;
        }

        String sourceFile = new File(downloadsDir.toString(), archiveFileName).getAbsolutePath();
        MainActivity.getPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, activity);
        publishProgress(Integer.toString(0), Integer.toString(1), archiveFileName, "");

        // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27
        final String baseName = Files.getNameWithoutExtension(Files.getNameWithoutExtension(archiveFileName)).split("__")[0];
        File destDirFile = new File(dictDir.toString(), baseName);
        final String initialDestDir = destDirFile.getAbsolutePath();

        Log.d(LOGGER_NAME, "Exists " + destDirFile.exists() + " isDir " + destDirFile.isDirectory());
        if (destDirFile.exists()) {
            Log.i(LOGGER_NAME, "Cleaning " + initialDestDir);
            cleanDirectory(destDirFile);
        } else {
            Log.i(LOGGER_NAME, "Creating afresh the directory " + destDirFile.mkdirs());
        }
        Log.d(LOGGER_NAME, "Exists " + destDirFile.exists() + " isDir " + destDirFile.isDirectory());

        String message2 = "Destination directory " + initialDestDir;
        Log.d(LOGGER_NAME, message2);
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
                Log.d(LOGGER_NAME, "isResourceFile " + isResourceFile);
                String message3 = "Destination: " + destFileName + "\nArchive entry: " + currentEntry.getName();
                Log.d(LOGGER_NAME, message3);
                if (isResourceFile && !resourceDirFile.exists()) {
                    Log.i(LOGGER_NAME, "Creating afresh the resource directory " + resourceDirFile.mkdirs());
                }
                String destFileDir = initialDestDir;
                if (isResourceFile) {
                    destFileDir = resourceDirFile.getAbsolutePath();
                }

                if (isResourceFile || destFileExtension.matches(VALID_NON_RESOURCE_FILE_EXTENSIONS_REGEX)) {
                    String destFile = new File(destFileDir, destFileName).getAbsolutePath();
                    if (!isResourceFile && destFileExtension.matches(VALID_NON_RESOURCE_FILE_EXTENSIONS_REGEX)) {
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
                    Log.w(LOGGER_NAME, "Not extracting " + currentEntry.getName());
                }
            }
            archiveInputStream.close();
            if (baseNameAccordingToArchiveEntries != null && !baseNameAccordingToArchiveEntries.equals(baseName)) {
                Log.d(LOGGER_NAME, "baseName: " + baseName + ", baseNameAccordingToArchiveEntries: " + baseNameAccordingToArchiveEntries);
                final String finalDestDir = new File(dictDir.toString(), baseNameAccordingToArchiveEntries).getAbsolutePath();
                Log.d(LOGGER_NAME, "destDirFile: " + destDirFile.toString() + ", finalDestDir: " + finalDestDir);
                final File finalDestDirFile = new File(finalDestDir);
                if (finalDestDirFile.exists()) {
                    cleanDirectory(finalDestDirFile);
                    Log.w(LOGGER_NAME, "Deleting preexisting dict directory with result: " + finalDestDirFile.delete());
                }
                final boolean result = destDirFile.renameTo(finalDestDirFile);
                Log.w(LOGGER_NAME, "Renaming the dict directory with result: " + result);
            }


            dictInfo.status = DictStatus.EXTRACTION_SUCCESS;
            Log.d(LOGGER_NAME, "success!");
            activity.storeDictVersion(archiveFileName);
        } catch (Exception e) {
            Log.e(LOGGER_NAME, "IOEx:", e);
            dictInfo.status = DictStatus.EXTRACTION_FAILURE;
        }
        deleteTarFile(sourceFile);

    }

    @Override
    protected Void doInBackground(Void... params) {
        for(DictInfo dictInfo : dictIndexStore.dictionariesSelectedMap.values()) {
            extractFile(dictInfo);
        }
        return null;
    }
}
