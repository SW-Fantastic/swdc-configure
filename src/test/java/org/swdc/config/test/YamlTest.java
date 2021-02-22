package org.swdc.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.config.configs.YamlConfig;

import java.io.File;

public class YamlTest {

    @Test
    public void testYaml() {

        YamlConfig config = new YamlConfig(new File("testConf.yml"));
        Assertions.assertNotNull(config);
        Assertions.assertEquals("val", config.getConfig("test",String.class));
        Assertions.assertEquals(123,config.getConfig("test2.num",int.class));

        YamlConfig subConf = (YamlConfig)config.getConfig("test2");
        Assertions.assertNotNull(subConf);
        Assertions.assertEquals("testVal",subConf.getConfig("aaa",String.class));
    }

    @Test
    public void testYamlModify() {
        YamlConfig config = new YamlConfig(new File("testConf.yml"));
        config.setConfig("test","valTest");
        config.setConfig("test2.num",456);

        YamlConfig subConf = (YamlConfig)config.getConfig("test2");
        Assertions.assertEquals("valTest",config.getConfig("test",String.class));
        Assertions.assertNotNull(subConf);
        Assertions.assertEquals(456,subConf.getConfig("num",int.class));

        //subConf.save();
    }

}
