package sanskritcode.sanskritdictionaryupdater;

import android.util.Log;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


class DictIndexStore implements Serializable {
    private final String index_indexorum = "https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md";
    // DictIndexStore must be serializable, hence we use specific class names below.
    final Map<String, String> indexUrls = new LinkedHashMap<>();
    final BiMap<String, String> indexesSelected = HashBiMap.create(100);
    Map<String, List<String>> indexedDicts = new LinkedHashMap<>();
    Set<String> dictionariesSelectedSet = new HashSet<>();
    final ArrayList<String> dictionariesSelectedLst = new ArrayList<>();
    final List<String> downloadedArchiveBasenames = new ArrayList<>();
    List<Boolean> dictFailure = new ArrayList<>();
    int autoUnselectedDicts = 0;

    int estimateDictionariesSelectedMBs() {
        int size = 0;
        for (String dictUrl : dictionariesSelectedSet) {
            size += DictNameHelper.getSize(dictUrl);
        }
        return size;
    }

    void getIndicesAddCheckboxes(final MainActivity activity) {
        if (indexUrls.isEmpty()) {
            final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
            asyncHttpClient.setEnableRedirects(true, true, true);
            asyncHttpClient.get(index_indexorum, new TextHttpResponseHandler() {
                private final String CLASS_NAME = this.getClass().getName();
                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                    Log.e(CLASS_NAME, "getIndices", throwable);
                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                    for (String line: responseString.split("\n")) {
                        String url = line.replace("<", "").replace(">", "");
                        String name = url.replaceAll("https://raw.githubusercontent.com/|/tars/tars.MD|master/", "");
                        indexUrls.put(name, url);
                        //noinspection ResultOfMethodCallIgnored
                        indexesSelected.put(name, url);
                        Log.d(getClass().getName(), activity.getString(R.string.added_index_url) + url);
                    }
                    activity.addCheckboxes(indexUrls, indexesSelected);
                }
            });
        } else {
            activity.addCheckboxes(indexUrls, indexesSelected);
        }
    }

    // Populates indexedDicts
    void getIndexedDictsSetCheckboxes(final GetUrlActivity getUrlActivity) {
        if (indexedDicts.isEmpty()) {
            final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
            indexedDicts = new LinkedHashMap<>();
            Log.i(getClass().getName(), "Will get dictionaries from " + indexesSelected.size());
            asyncHttpClient.setEnableRedirects(true, true, true);
            for (String name : indexesSelected.keySet()) {
                final String url = indexesSelected.get(name);
                try {
                    asyncHttpClient.get(url, new TextHttpResponseHandler() {
                        final String LOGGER_NAME = "getIndexedDicts";

                        @Override
                        public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable){
                            Log.e(LOGGER_NAME, "Failed ", throwable);
                            String message = String.format(getUrlActivity.getString(R.string.index_download_failed), url);
                            getUrlActivity.topText.setText(message);
                            // The below would result in
                            // java.lang.RuntimeException: android.os.FileUriExposedException: file:///storage/emulated/0/logcat.txt exposed beyond app through ClipData.Item.getUri()
                            // A known issue: https://github.com/loopj/android-async-http/issues/891
                            // getUrlActivity.sendLoagcatMail();
                        }

                        @Override
                        public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                            List<String> urls = new ArrayList<>();
                            for (String line : responseString.split("\n")) {
                                String dictUrl = line.replace("<", "").replace(">", "");
                                urls.add(dictUrl);
                                Log.d(LOGGER_NAME, getUrlActivity.getApplicationContext().getString(R.string.added_dictionary_url) + dictUrl);
                            }
                            Log.d(LOGGER_NAME, "Index handled: " + url);
                            indexedDicts.put(indexesSelected.inverse().get(url), urls);

                            if (indexesSelected.size() == indexedDicts.size()) {
                                getUrlActivity.addCheckboxes(indexedDicts, null);
                            }
                        }
                    });
                } catch (Throwable throwable) {
                    Log.e(getClass().getName(), "error with " + url, throwable);
                    String message = String.format(getUrlActivity.getString(R.string.index_download_failed), url);
                    getUrlActivity.topText.setText(message);
                    getUrlActivity.sendLoagcatMail();
                }
            }
        } else {
            getUrlActivity.addCheckboxes(indexedDicts, dictionariesSelectedSet);
        }
    }
}
