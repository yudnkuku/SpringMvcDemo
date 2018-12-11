package demo;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackTest {

    @Test
    public void testSlf4j() {
        Logger logger = LoggerFactory.getLogger("logger");
        logger.trace("===trace===");
        logger.debug("===debug===");
        logger.info("===info===");
        logger.warn("===warn===");
        logger.error("===error===");
    }
}
