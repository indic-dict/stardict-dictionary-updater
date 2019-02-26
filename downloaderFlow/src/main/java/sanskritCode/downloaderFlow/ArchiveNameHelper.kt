@file:Suppress("UnstableApiUsage")

package sanskritCode.downloaderFlow

import com.google.common.io.Files


internal object ArchiveNameHelper {

    fun getNameWithoutAnyExtension(somePath: String): String {
        // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27.tar.gz
        // Hence calling getBaseName twice.
        var baseName = Files.getNameWithoutExtension(somePath)
        while (!Files.getFileExtension(baseName).isEmpty()) {
            baseName = Files.getNameWithoutExtension(baseName)
        }
        return baseName
    }

    fun getArchiveNameAndVersion(fileName: String): Array<String> {
        return getNameWithoutAnyExtension(fileName).split("__".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
    }

    // handle filenames of the type: kRdanta-rUpa-mAlA__2016-02-20_23-22-27
    fun getUnversionedArchiveBaseName(somePath: String) = getArchiveNameAndVersion(somePath)[0]

    fun getVersion(somePath: String, defaultVersion: String = "0000"): String {
        val archiveNameParts = getArchiveNameAndVersion(somePath)
        return if (archiveNameParts.size > 1) {
            getArchiveNameAndVersion(somePath)[1]
        } else {
            defaultVersion
        }
    }

    fun getSize(fileName: String): Int {
        val archiveNameParts = getArchiveNameAndVersion(fileName)
        return if (archiveNameParts.size > 2) {
            Integer.parseInt(archiveNameParts[2].replace("MB".toRegex(), "")) + 1
        } else {
            1
        }
    }
}
