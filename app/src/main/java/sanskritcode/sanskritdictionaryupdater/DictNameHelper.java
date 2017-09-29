package sanskritcode.sanskritdictionaryupdater;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.google.common.io.Files;


class DictNameHelper {

    @NonNull
    public static String getNameWithoutAnyExtension(String somePath) {
        String baseName = Files.getNameWithoutExtension(somePath);
        while (!Files.getFileExtension(baseName).isEmpty()) {
            baseName = Files.getNameWithoutExtension(baseName);
        }
        return baseName;
    }

    public static String[] getDictNameAndVersion(String fileName) {
        // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27.tar.gz
        // Hence calling getBaseName twice.
        return getNameWithoutAnyExtension(fileName).split("__");
    }
}
