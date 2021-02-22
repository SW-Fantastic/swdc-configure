# application - config

配置的处理器，可以快速加载各类外部配置，主要处理配置的读写。
将会在JitPack提供。

预计适配这些类型的配置文件：

 - [x] properties
 - [x] yml
 - [x] xml
 - [x] json
 - [ ] ini

提供文件的加载，存储，写入，以及和java对象之间的映射关系，为Core模块
的启动环境提供必要的基础。

## 如何使用

假定有如下配置：
```yaml
test2:
  num: 123
  aaa: testVal
  bbb: test2
  ccc: [itemA, itemB, 111]
test: val
```
我们可以定义以下配置类：

```java
 public class SubConfigTestClass extends AbstractConfigure {

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

    public class ConfigureTestClass extends AbstractConfigure {

        @Property("test")
        private String test;

        @Property("test2.num")
        private Integer testA;

        // 这里演示配置类的嵌套
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
```

那么我们只需要这样：
```
   Configure configure = new YamlConfig(new File("testConf.yml"));
   ConfigureTestClass testClass = new ConfigureTestClass(configure);
```
就可以加载配置文件了。