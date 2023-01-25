# application - config


配置的处理器，可以快速加载各类外部配置，主要处理配置的读写。


预计适配这些类型的配置文件：


 - [x] yml
 - [x] xml（不好用，过段时间会改掉它）
 - [x] json
 - [x] properties
 - [x] ini
 - [x] HOCON

提供文件的加载，存储，写入，以及和java对象之间的映射关系，为Core模块
的启动环境提供必要的基础。

各类配置文件的具体使用方法可以参考test内的例子，请不要直接运行整个test类，分别执行
里面的方法才能够获取正确的结果。

## 更新日志

我认为之前的设计有问题，正在重写此模块。 

2021 - 6 - 30

我已经重写了这一部分，但是没放在maven上面，基本的配置都已经完成了。 现在里面还有一个
十分简易的本地化功能，只是暂时在这里，以后可能会移除。

2021 - 7 - 5


## 如何使用
添加以下仓库：
```xml
<repositories>
	<repository>
	  <id>jitpack.io</id>
	  <url>https://jitpack.io</url>
	</repository>
</repositories>
```
然后使用此Maven：
```xml
<dependency>
	 <groupId>com.github.SW-Fantastic</groupId>
	 <artifactId>swdc-dependency</artifactId>
	 <version>0.1.0</version>
</dependency>
```

## 使用方法

假定有如下配置：
```json
{
  "test2": {
    "num": 123,
    "aaa": "testVal",
    "bbb": "test2",
    "ccc": ["itemA", "itemB", "111"]
  },
  "test": "val"
}

```
我们可以定义以下配置类：

```java
public class SubConfigTestClass extends AbstractConfig {

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

@ConfigureSource(value = "test.json", handler = JsonConfigHandler.class)
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
   ConfigureTestClass testClass = new ConfigureTestClass();
```
就可以加载配置文件了，配置文件父类自带save方法，修改后可以直接保存。