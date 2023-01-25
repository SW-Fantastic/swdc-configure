package org.swdc.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.config.AbstractConfig;
import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;
import org.swdc.config.configs.HOCONConfigHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestHocon {

    public static class TestConfig2  {

        @Property("testC")
        private String testC;

        @Property("testD")
        private String testD;

        public String getTestC() {
            return testC;
        }

        public String getTestD() {
            return testD;
        }

        public void setTestC(String testC) {
            this.testC = testC;
        }

        public void setTestD(String testD) {
            this.testD = testD;
        }
    }

    @ConfigureSource(value = "test.conf",handler = HOCONConfigHandler.class)
    public static class TestConfig extends AbstractConfig {

        @Property("test.bbb")
        private List<Integer> testB;

        @Property("test.aaa")
        private String testA;

        @Property("test.ccc")
        private List<String> testC;

        @Property("test.ddd")
        private Map<String,Integer> testD;

        @Property("test.conf")
        private TestConfig2 config2;

        public TestConfig2 getConfig2() {
            return config2;
        }

        public void setConfig2(TestConfig2 config2) {
            this.config2 = config2;
        }

        public String getTestA() {
            return testA;
        }

        public void setTestA(String testA) {
            this.testA = testA;
        }

        public void setTestB(List<Integer> testB) {
            this.testB = testB;
        }

        public List<Integer> getTestB() {
            return testB;
        }

        public List<String> getTestC() {
            return testC;
        }

        public void setTestC(List<String> testC) {
            this.testC = testC;
        }

        public Map<String, Integer> getTestD() {
            return testD;
        }

        public void setTestD(Map<String, Integer> testD) {
            this.testD = testD;
        }
    }

    @Test
    public void testLoad() throws IOException {
        TestConfig config = new TestConfig();
        Assertions.assertEquals("1234",config.getTestA());
    }

    @Test
    public void testSave() {
        TestConfig config = new TestConfig();
        config.setTestA("4321");
        config.save();

        config.setTestA("1234");
        config.save();
    }

}
