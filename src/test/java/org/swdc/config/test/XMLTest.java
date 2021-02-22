package org.swdc.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.config.configs.XmlConfig;

import java.io.File;

public class XMLTest {

    @Test
    public void testXmlRead() {
        XmlConfig config = new XmlConfig(new File("testConfig.xml"));
        Assertions.assertNotNull(config);
        Assertions.assertEquals("test",config.getConfig("test",String.class));
        Assertions.assertEquals("这个是可以层叠嵌套的",config.getConfig("test.testCascade",String.class));

        XmlConfig subConf = (XmlConfig) config.getConfig("test");
        Assertions.assertNotNull(subConf);
        Assertions.assertEquals("这个是可以层叠嵌套的",subConf.getConfig("testCascade",String.class));
    }

    @Test
    public void testXmlModify() {
        XmlConfig config = new XmlConfig(new File("testConfig.xml"));
        config.setConfig("test.testCascade","11111");
        Assertions.assertEquals("11111",config.getConfig("test.testCascade",String.class));
        XmlConfig subConf = (XmlConfig) config.getConfig("test");
        Assertions.assertEquals("11111",subConf.getConfig("testCascade",String.class));
        //config.save();
    }

}
