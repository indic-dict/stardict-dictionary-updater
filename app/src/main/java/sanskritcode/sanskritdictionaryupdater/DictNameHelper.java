package sanskritcode.sanskritdictionaryupdater;

import android.support.annotation.NonNull;

import com.google.common.io.Files;


class DictNameHelper {

    @NonNull
    static String getNameWithoutAnyExtension(String somePath) {
        // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27.tar.gz
        // Hence calling getBaseName twice.
        String baseName = Files.getNameWithoutExtension(somePath);
        while (!Files.getFileExtension(baseName).isEmpty()) {
            baseName = Files.getNameWithoutExtension(baseName);
        }
        return baseName;
    }

    static String[] getDictNameAndVersion(String fileName) {
        return getNameWithoutAnyExtension(fileName).split("__");
    }

    static int getSize(String fileName) {
        String[] dictnameParts = DictNameHelper.getDictNameAndVersion(fileName);
        if (dictnameParts.length > 2) {
            return Integer.parseInt(dictnameParts[2].replaceAll("MB", "")) + 1;
        } else {
            return 1;
        }
    }
}
