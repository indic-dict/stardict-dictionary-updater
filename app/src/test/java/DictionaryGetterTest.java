import com.google.common.io.Files;

import org.junit.Test;

import java.util.logging.Logger;

public class DictionaryGetterTest {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * A playground to test various API-s.
     */
    @Test
    public void test_playground() {
        try {
            String url = "https://github.com/sanskrit-coders/stardict-sanskrit/releases/download/2017-04-14/Meulenbeld-Sanskrit-Names-of-Plants__2017-04-17_08-29-39__0MB.tar.gz";
            logger.info(
                    Files.getNameWithoutExtension(url));
            final String fileName = url.substring(url.lastIndexOf("/")).replace("/", "");
            logger.info(fileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("@Test - test_method_1");
    }
}
