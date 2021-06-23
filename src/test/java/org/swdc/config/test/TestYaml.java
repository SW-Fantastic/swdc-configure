package org.swdc.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;
import org.swdc.config.AbstractConfig;
import org.swdc.config.configs.YamlConfigHandler;

import java.io.IOException;
import java.util.List;

public class TestYaml {

    public static class TestConfig2  {

        private String testC;

        private String testD;

        private List<Integer> list;

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

        public List<Integer> getList() {
            return list;
        }

        public void setList(List<Integer> list) {
            this.list = list;
        }
    }

    @ConfigureSource(value = "test.yaml",handler = YamlConfigHandler.class)
    public static class TestConfig extends AbstractConfig {

        @Property("test.bbb")
        private String testB;

        @Property("test.aaa")
        private String testA;

        @Property("test.conf")
        private TestConfig2 config2;

        public String getTestA() {
            return testA;
        }

        public String getTestB() {
            return testB;
        }

        public void setTestA(String testA) {
            this.testA = testA;
        }

        public void setTestB(String testB) {
            this.testB = testB;
        }

        public TestConfig2 getConfig2() {
            return config2;
        }
    }

    @Test
    public void testYamlLoad(){
        TestConfig config = new TestConfig();
        Assertions.assertEquals("1234",config.getTestA());
        Assertions.assertNotNull(config.getConfig2());
    }

    @Test
    public void testSave() throws IOException {
        TestConfig config = new TestConfig();

        config.setTestA("4567");
        config.save();

        config.load();
        Assertions.assertNotEquals("1234",config.getTestA());

        config.setTestA("1234");
        config.save();

        config.load();
        Assertions.assertEquals("1234",config.getTestA());
    }

}
