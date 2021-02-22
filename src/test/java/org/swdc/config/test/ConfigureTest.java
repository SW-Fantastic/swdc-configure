package org.swdc.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.config.Configure;
import org.swdc.config.AbstractConfigure;
import org.swdc.config.annotations.Property;
import org.swdc.config.configs.YamlConfig;

import java.io.File;

public class ConfigureTest {

    public static class SubConfigTestClass extends AbstractConfigure {

        @Property("aaa")
        private String testOne;

        @Property("bbb")
        private String testTwo;

        @Property("num")
        private Integer testThree;

        public SubConfigTestClass(Configure configure) {
            super(configure);
        }

        public Integer getTestThree() {
            return testThree;
        }

        public String getTestOne() {
            return testOne;
        }

        public String getTestTwo() {
            return testTwo;
        }

        public void setTestOne(String testOne) {
            this.testOne = testOne;
        }

        public void setTestThree(Integer testThree) {
            this.testThree = testThree;
        }

        public void setTestTwo(String testTwo) {
            this.testTwo = testTwo;
        }
    }

    public static class ConfigureTestClass extends AbstractConfigure {

        @Property("test")
        private String test;

        @Property("test2.num")
        private Integer testA;

        @Property("test2")
        private SubConfigTestClass subConfigTestClass;

        public ConfigureTestClass(Configure configure) {
            super(configure);
        }

        public Integer getTestA() {
            return testA;
        }

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public void setTestA(Integer testA) {
            this.testA = testA;
        }

        public SubConfigTestClass getSubConfigTestClass() {
            return subConfigTestClass;
        }

        public void setSubConfigTestClass(SubConfigTestClass subConfigTestClass) {
            this.subConfigTestClass = subConfigTestClass;
        }
    }

    @Test
    public void testConfig() {
        Configure configure = new YamlConfig(new File("testConf.yml"));
        ConfigureTestClass testClass = new ConfigureTestClass(configure);
        Assertions.assertEquals("val",testClass.getTest());
        Assertions.assertEquals(123,testClass.getTestA());

        SubConfigTestClass subConfig = testClass.getSubConfigTestClass();
        Assertions.assertNotNull(subConfig);
        Assertions.assertEquals(123, subConfig.getTestThree());

        subConfig.save();
    }

}
