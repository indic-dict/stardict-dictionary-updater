@file:Suppress("UnstableApiUsage")

package sanskritCode.downloaderFlow

import android.util.Log

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.io.Files
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.TextHttpResponseHandler

import java.io.Serializable
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File


enum class ArchiveStatus {
    NOT_TRIED, DOWNLOAD_SUCCESS, DOWNLOAD_FAILURE, EXTRACTION_SUCCESS, EXTRACTION_FAILURE
}

class ArchiveInfo(var archiveInfoJsonStr: String) : Serializable {
    var status = ArchiveStatus.NOT_TRIED
    val archiveInfoJsonObject = JsonParser().parse(archiveInfoJsonStr).asJsonObject
    var url: String = archiveInfoJsonObject.get("url").asString

    fun getDownloadedArchiveBasename() : String {
        return url.substring(url.lastIndexOf("/")).replace("/", "")
    }

    // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27
    internal fun getUnversionedArchiveBaseName() =
            Files.getNameWithoutExtension(
                    Files.getNameWithoutExtension(
                            getDownloadedArchiveBasename())).split("__".toRegex())
                    .dropLastWhile({ it.isEmpty() }).toTypedArray()[0]

    override fun toString(): String {
        return ("\n status: " + status
                + "\n downloadedArchiveBasename: " + getDownloadedArchiveBasename()
                + "\n url: " + url
                + "\n archiveInfoJson: " + archiveInfoJsonStr)
    }

    fun getDestinationPathSuffix() : String {
        if (archiveInfoJsonObject.has("destinationPathSuffix")) {
            return archiveInfoJsonObject.get("destinationPathSuffix").asString
        } else {
            return getUnversionedArchiveBaseName()
        }
    }

    companion object {
        fun fromUrl(url: String) : ArchiveInfo = ArchiveInfo("{\"url\": \"$url\"}")
    }
}

class ArchiveIndexStore(val indexIndexorum: String) : Serializable {
    private val LOGGER_TAG = javaClass.getSimpleName()
    // ArchiveIndexStore must be serializable, hence we use specific class names below.
    private val indexUrls = LinkedHashMap<String, String>()
    // the indexes which were selected by the user.
    val indexesSelected: BiMap<String, String> = HashBiMap.create(100)
    // maps each archive index to the list of archives listed in that index.
    var indexedArchives: MutableMap<String, List<ArchiveInfo>> = LinkedHashMap()
    var archivesSelectedMap: MutableMap<String, ArchiveInfo> = HashMap()
    var autoUnselectedArchives = 0

    override fun toString(): String {
        return ("\nindexIndexorum: " + indexIndexorum
                + "\nindexUrls: " + indexUrls
                + "\nindexesSelected: " + indexesSelected
                + "\nindexedArchives: " + indexedArchives
                + "\nautoUnselectedArchives: " + autoUnselectedArchives
                + "\narchivesSelectedMap: " + archivesSelectedMap)
    }


    fun estimateArchivesSelectedMBs(): Int {
        var size = 0
        for (url in archivesSelectedMap.keys) {
            size += ArchiveNameHelper.getSize(url)
        }
        return size
    }

    fun getIndicesAddCheckboxes(activity: MainActivity) {
        if (indexUrls.isEmpty()) {
            val asyncHttpClient = AsyncHttpClient()
            asyncHttpClient.setEnableRedirects(true, true, true)
            asyncHttpClient.get(indexIndexorum, object : TextHttpResponseHandler() {
                override fun onFailure(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String, throwable: Throwable) {
                    Log.e(LOGGER_TAG, ":getIndicesAddCheckboxes:" + "getIndices", throwable)
                }

                override fun onSuccess(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String) {
                    for (line in responseString.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                        val url = line.replace("<", "").replace(">", "")
                        val name = url.replace(activity.getString(R.string.df_archive_url_redundant_string_regex).toRegex(), "")
                        indexUrls[name] = url

                        indexesSelected[name] = url
                        Log.d(LOGGER_TAG, ":getIndicesAddCheckboxes:" + activity.getString(R.string.df_added_index_url) + url)
                    }
                    activity.addCheckboxes(indexUrls, indexesSelected)
                }
            })
        } else {
            activity.addCheckboxes(indexUrls, indexesSelected)
        }
    }

    /**
     * Populates indexedArchives
     * getUrlActivity gets archive urls and displays them for the user to choose.
     */
    fun getIndexedArchivesSetCheckboxes(getUrlActivity: GetUrlActivity) {
        // In some contexts, indexedArchives may already be populated.
        if (indexedArchives.isEmpty()) {
            val asyncHttpClient = AsyncHttpClient()
            indexedArchives = LinkedHashMap()
            Log.i(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:" + "Will get archives from " + indexesSelected.size)
            asyncHttpClient.setEnableRedirects(true, true, true)
            for (name in indexesSelected.keys) {
                val url = indexesSelected[name]
                val archiveIndexStore = this

                try {
                    asyncHttpClient.get(url, object : TextHttpResponseHandler() {
                        override fun onFailure(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String, throwable: Throwable) {
                            Log.e(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:" + "Failed ", throwable)
                            BaseActivity.largeLog(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:" + archiveIndexStore.toString())
                            val message = String.format(getUrlActivity.getString(R.string.df_index_download_failed), url)
                            getUrlActivity.topText?.setText(message)
                            // The below would result in
                            // java.lang.RuntimeException: android.os.FileUriExposedException: file:///storage/emulated/0/logcat.txt exposed beyond app through ClipData.Item.getUri()
                            // A known issue: https://github.com/loopj/android-async-http/issues/891
                            // getUrlActivity.sendLoagcatMail();

                            // Just proceed with the next dict index.
                            indexedArchives[name] = listOf<ArchiveInfo>(ArchiveInfo.fromUrl(url!!.replace("[_/.]+".toRegex(), "_") + "_indexGettingFailed"))
                            if (indexesSelected.size == indexedArchives.size) {
                                getUrlActivity.addCheckboxes(indexedArchives, null)
                            }
                        }

                        fun processMdFileContents(responseString: String): ArrayList<ArchiveInfo> {
                            val archiveInfos = ArrayList<ArchiveInfo>()
                            for (line in responseString.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                                val url1 = line.replace("<", "").replace(">", "")
                                archiveInfos.add(ArchiveInfo.fromUrl(url1))
                                Log.d(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes: Added archive: " + url1)
                            }
                            return archiveInfos

                        }

                        fun processJsonFileContents(responseString: String): ArrayList<ArchiveInfo> {
                            val archiveInfos = ArrayList<ArchiveInfo>()
                            val parser = JsonParser()
                            val archiveArray = parser.parse(responseString).getAsJsonArray()
                            archiveArray.forEach({
                                val archiveInfoString = it.asJsonObject.toString()
                                Log.d(LOGGER_TAG, archiveInfoString)
                                val archiveInfo = ArchiveInfo(archiveInfoString)
                                archiveInfos.add(archiveInfo)
                            })
                            return archiveInfos
                        }

                        override fun onSuccess(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String) {
                            var archiveInfos : List<ArchiveInfo> = listOf()
                            if(url.toString().toLowerCase().endsWith(".md")) {
                                archiveInfos = processMdFileContents(responseString = responseString)
                            } else if(url.toString().toLowerCase().endsWith(".json")) {
                                archiveInfos = processJsonFileContents(responseString = responseString)
                            }
                            Log.d(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:Index handled: $url")
                            indexedArchives[indexesSelected.inverse()[url]!!] = archiveInfos

                            if (indexesSelected.size == indexedArchives.size) {
                                getUrlActivity.addCheckboxes(indexedArchives, null)
                            }

                        }
                    })
                } catch (throwable: Throwable) {
                    Log.e(LOGGER_TAG, ":getIndexedArchivesSetCheckboxes:error with $url", throwable)
                    val message = String.format(getUrlActivity.getString(R.string.df_index_download_failed), url)
                    getUrlActivity.topText?.setText(message)
                    getUrlActivity.sendLoagcatMail()
                }

            }
        } else {
            getUrlActivity.addCheckboxes(indexedArchives, archivesSelectedMap.keys)
        }
    }
}
