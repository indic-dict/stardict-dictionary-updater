import com.google.common.io.Files

import org.junit.Test

import java.util.logging.Logger

class DictionaryGetterTest {
    private val logger = Logger.getLogger(this.javaClass.getName())

    /**
     * A playground to test various API-s.
     */
    @Test
    fun test_playground() {
        try {
            val url = "https://github.com/sanskrit-coders/stardict-sanskrit/releases/download/2017-04-14/Meulenbeld-Sanskrit-Names-of-Plants__2017-04-17_08-29-39__0MB.tar.gz"
            @Suppress("UnstableApiUsage")
            logger.info(
                    Files.getNameWithoutExtension(url))
            val fileName = url.substring(url.lastIndexOf("/")).replace("/", "")
            logger.info(fileName)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        logger.info("@Test - test_method_1")
    }
}
