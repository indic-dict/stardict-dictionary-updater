package sanskritCode.downloaderFlow

import com.google.common.io.Files


internal object DictNameHelper {

    fun getNameWithoutAnyExtension(somePath: String): String {
        // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27.tar.gz
        // Hence calling getBaseName twice.
        var baseName = Files.getNameWithoutExtension(somePath)
        while (!Files.getFileExtension(baseName).isEmpty()) {
            baseName = Files.getNameWithoutExtension(baseName)
        }
        return baseName
    }

    fun getDictNameAndVersion(fileName: String): Array<String> {
        return getNameWithoutAnyExtension(fileName).split("__".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
    }

    fun getSize(fileName: String): Int {
        val dictnameParts = getDictNameAndVersion(fileName)
        return if (dictnameParts.size > 2) {
            Integer.parseInt(dictnameParts[2].replace("MB".toRegex(), "")) + 1
        } else {
            1
        }
    }
}
