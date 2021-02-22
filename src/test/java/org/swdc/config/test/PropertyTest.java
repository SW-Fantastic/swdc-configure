package org.swdc.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.config.Configure;
import org.swdc.config.configs.PropertiesConfig;

import java.io.File;

public class PropertyTest {

    @Test
    public void testPropertyRead() {
        Configure configure = new PropertiesConfig(new File("config.properties"));
        Assertions.assertEquals("123",configure.getConfig("app.testA",String.class));
        Configure configTest = configure.getConfig("app.testB");
        Assertions.assertNotNull(configTest);
        Assertions.assertEquals("111",configTest.getConfig("aaa",String.class));
        Assertions.assertEquals(111,configTest.getConfig("aaa",int.class));
    }

    @Test
    public void testPropertyModify() {
        Configure configure = new PropertiesConfig(new File("config.properties"));
        configure.setConfig("app.testA","abcde");

        Configure configTest = configure.getConfig("app.testB");
        configure.setConfig("app.testB.aaa","edited");

        Assertions.assertEquals("abcde",configure.getConfig("app.testA",String.class));
        Assertions.assertEquals("edited",configTest.getConfig("aaa",String.class));
    }

}
