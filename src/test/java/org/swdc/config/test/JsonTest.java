package org.swdc.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.config.configs.JsonConfig;

import java.io.File;

public class JsonTest {

    @Test
    public void testJsonRead(){
        JsonConfig config = new JsonConfig(new File("testConf.json"));
        Assertions.assertNotNull(config);

        Assertions.assertEquals("value",config.getConfig("test",String.class));
        JsonConfig subConfig = (JsonConfig) config.getConfig("testObj");
        Assertions.assertNotNull(subConfig);
        Assertions.assertEquals("testAAA",subConfig.getConfig("aaa",String.class));
    }

    @Test
    public void testJsonModify(){
        JsonConfig config = new JsonConfig(new File("testConf.json"));
        config.setConfig("test","valuetest");
        config.setConfig("testObj.aaa","testVal");

        Assertions.assertEquals("valuetest",config.getConfig("test",String.class));
        JsonConfig subConfig = (JsonConfig) config.getConfig("testObj");
        Assertions.assertEquals("testVal",subConfig.getConfig("aaa",String.class));
    }

}
