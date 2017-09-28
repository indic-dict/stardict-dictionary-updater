import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;

public class DictionaryGetterTest {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    @Test
    public void test_url_encoding() {
        try {
            logger.info(URLEncoder.encode("https://github.com/sanskrit-coders/stardict-sanskrit/releases/download/2017-04-14/Meulenbeld-Sanskrit-Names-of-Plants__2017-04-17_08-29-39__0MB.tar.gz", "UTF8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        logger.info("@Test - test_method_1");
    }
}
