package org.swdc.config.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.config.AbstractConfig;
import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;
import org.swdc.config.configs.XmlConfigHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TestXml {


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

    @ConfigureSource(value = "test.xml",handler = XmlConfigHandler.class)
    public static class TestConfig extends AbstractConfig {

        @Property("test.bbb")
        private String testB;

        @Property("test.aaa")
        private Integer testA;

        @Property("test.conf")
        private TestConfig2 config2;


        public Integer getTestA() {
            return testA;
        }

        public void setTestA(Integer testA) {
            this.testA = testA;
        }

        public String getTestB() {
            return testB;
        }


        public void setTestB(String testB) {
            this.testB = testB;
        }

        public TestConfig2 getConfig2() {
            return config2;
        }

        public void setConfig2(TestConfig2 config2) {
            this.config2 = config2;
        }
    }


    @Test
    public void testXStream() throws IOException {

        XStream xmlStream = new XStream();
        xmlStream.alias("configure",Map.class);
        xmlStream.registerConverter(new XmlConfigHandler.MapEntryConverter());
        Map<String,Object> result = (Map<String, Object>) xmlStream.fromXML(new File("test.xml"));
        System.out.println(result);

        /*result.put("conf",new TestConfig2());
        System.out.println(xmlStream.toXML(result));*/


    }

    @Test
    public void testLoadXml(){
        TestConfig config = new TestConfig();
        Assertions.assertNotNull(config.getConfig2());
        Assertions.assertEquals(1234,config.getTestA());

    }

    @Test
    public void testSaveXml() {
        TestConfig config = new TestConfig();

        config.setTestA(4567);
        config.save();

        config.load();
        Assertions.assertNotEquals(1234,config.getTestA());

        config.setTestA(1234);
        config.save();

        config.load();
        Assertions.assertEquals(1234,config.getTestA());
    }

}
