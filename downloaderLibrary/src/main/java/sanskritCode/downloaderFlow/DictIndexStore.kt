package sanskritCode.downloaderFlow

import android.util.Log

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.TextHttpResponseHandler
import sanskritCode.downloaderFlow.DictNameHelper
import sanskritCode.downloaderFlow.GetUrlActivity
import sanskritCode.downloaderFlow.MainActivity
import sanskritCode.downloaderFlow.R

import java.io.Serializable
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap

enum class DictStatus {
    NOT_TRIED, DOWNLOAD_SUCCESS, DOWNLOAD_FAILURE, EXTRACTION_SUCCESS, EXTRACTION_FAILURE
}

class DictInfo(var url: String) : Serializable {
    var status = DictStatus.NOT_TRIED
    var downloadedArchiveBasename: String? = null

    override fun toString(): String {
        return ("\n status: " + status
                + "\n downloadedArchiveBasename: " + downloadedArchiveBasename
                + "\n url: " + url)
    }
}

class DictIndexStore : Serializable {
    private val LOGGER_TAG = javaClass.getSimpleName()
    private val index_indexorum = "https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md"
    // DictIndexStore must be serializable, hence we use specific class names below.
    private val indexUrls = LinkedHashMap<String, String>()
    val indexesSelected: BiMap<String, String> = HashBiMap.create(100)
    var indexedDicts: MutableMap<String, List<String>> = LinkedHashMap()
    var dictionariesSelectedMap: MutableMap<String, DictInfo> = HashMap()
    var autoUnselectedDicts = 0

    override fun toString(): String {
        return ("\nindex_indexorum: " + index_indexorum
                + "\nindexUrls: " + indexUrls
                + "\nindexesSelected: " + indexesSelected
                + "\nindexedDicts: " + indexedDicts
                + "\nautoUnselectedDicts: " + autoUnselectedDicts
                + "\ndictionariesSelectedMap: " + dictionariesSelectedMap)
    }


    fun estimateDictionariesSelectedMBs(): Int {
        var size = 0
        for (dictUrl in dictionariesSelectedMap.keys) {
            size += DictNameHelper.getSize(dictUrl)
        }
        return size
    }

    fun getIndicesAddCheckboxes(activity: MainActivity) {
        if (indexUrls.isEmpty()) {
            val asyncHttpClient = AsyncHttpClient()
            asyncHttpClient.setEnableRedirects(true, true, true)
            asyncHttpClient.get(index_indexorum, object : TextHttpResponseHandler() {
                override fun onFailure(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String, throwable: Throwable) {
                    Log.e(LOGGER_TAG, ":getIndicesAddCheckboxes:" + "getIndices", throwable)
                }

                override fun onSuccess(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String) {
                    for (line in responseString.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                        val url = line.replace("<", "").replace(">", "")
                        val name = url.replace("https://raw.githubusercontent.com/|/tars/tars.MD|master/".toRegex(), "")
                        indexUrls[name] = url

                        indexesSelected[name] = url
                        Log.d(LOGGER_TAG, ":getIndicesAddCheckboxes:" + activity.getString(R.string.added_index_url) + url)
                    }
                    activity.addCheckboxes(indexUrls, indexesSelected)
                }
            })
        } else {
            activity.addCheckboxes(indexUrls, indexesSelected)
        }
    }

    // Populates indexedDicts
    fun getIndexedDictsSetCheckboxes(getUrlActivity: GetUrlActivity) {
        if (indexedDicts.isEmpty()) {
            val asyncHttpClient = AsyncHttpClient()
            indexedDicts = LinkedHashMap()
            Log.i(LOGGER_TAG, ":getIndexedDictsSetCheckboxes:" + "Will get dictionaries from " + indexesSelected.size)
            asyncHttpClient.setEnableRedirects(true, true, true)
            for (name in indexesSelected.keys) {
                val url = indexesSelected[name]
                val dictIndexStore = this

                try {
                    asyncHttpClient.get(url, object : TextHttpResponseHandler() {
                        override fun onFailure(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String, throwable: Throwable) {
                            Log.e(LOGGER_TAG, ":getIndexedDictsSetCheckboxes:" + "Failed ", throwable)
                            BaseActivity.largeLog(LOGGER_TAG, ":getIndexedDictsSetCheckboxes:" + dictIndexStore.toString())
                            val message = String.format(getUrlActivity.getString(R.string.index_download_failed), url)
                            getUrlActivity.topText?.setText(message)
                            // The below would result in
                            // java.lang.RuntimeException: android.os.FileUriExposedException: file:///storage/emulated/0/logcat.txt exposed beyond app through ClipData.Item.getUri()
                            // A known issue: https://github.com/loopj/android-async-http/issues/891
                            // getUrlActivity.sendLoagcatMail();

                            // Just proceed with the next dict.
                            indexedDicts[name] = listOf<String>(url!!.replace("[_/.]+".toRegex(), "_") + "_indexGettingFailed")
                            if (indexesSelected.size == indexedDicts.size) {
                                getUrlActivity.addCheckboxes(indexedDicts, null)
                            }
                        }

                        override fun onSuccess(statusCode: Int, headers: Array<cz.msebera.android.httpclient.Header>, responseString: String) {
                            val urls = ArrayList<String>()
                            for (line in responseString.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                                val dictUrl = line.replace("<", "").replace(">", "")
                                urls.add(dictUrl)
                                Log.d(LOGGER_TAG, ":getIndexedDictsSetCheckboxes:" + getUrlActivity.applicationContext.getString(R.string.added_dictionary_url) + dictUrl)
                            }
                            Log.d(LOGGER_TAG, ":getIndexedDictsSetCheckboxes:Index handled: $url")
                            indexedDicts[indexesSelected.inverse()[url]!!] = urls

                            if (indexesSelected.size == indexedDicts.size) {
                                getUrlActivity.addCheckboxes(indexedDicts, null)
                            }
                        }
                    })
                } catch (throwable: Throwable) {
                    Log.e(LOGGER_TAG, ":getIndexedDictsSetCheckboxes:error with $url", throwable)
                    val message = String.format(getUrlActivity.getString(R.string.index_download_failed), url)
                    getUrlActivity.topText?.setText(message)
                    getUrlActivity.sendLoagcatMail()
                }

            }
        } else {
            getUrlActivity.addCheckboxes(indexedDicts, dictionariesSelectedMap.keys)
        }
    }
}
