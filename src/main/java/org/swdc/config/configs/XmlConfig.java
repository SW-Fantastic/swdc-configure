package org.swdc.config.configs;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.swdc.config.AbstractConfig;
import org.swdc.config.Configure;
import org.swdc.config.Converter;
import org.swdc.config.converters.Converters;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 来自Xml的配置。
 */
public class XmlConfig extends AbstractConfig {

    /**
     * 纯文本的内容
     */
    private Map<String,String> property;

    /**
     * 嵌套的内容
     */
    private Map<String, XmlConfig> children;

    /**
     * 标签的内容
     */
    private String content;

    private File file;
    private Path path;
    private Document document;

    /**
     * 通过inputStream创建只读的Xml格式配置。
     * @param in
     */
    public XmlConfig(InputStream in) {
        property = new HashMap<>();
        children = new HashMap<>();
        SAXReader reader = new SAXReader();
        try {
            document = reader.read(in);
            Element root = document.getRootElement();
            List<Element> elements = root.elements();
            for (Element element: elements) {
                if (element.elements() != null && element.elements().size() > 0) {
                    XmlConfig config = new XmlConfig(element);
                    config.setParent(this);
                    this.children.put(element.attributeValue("key"),config);
                } else {
                    String key = element.attributeValue("key");
                    String val = element.getText();
                    property.put(key,val);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("无法读取xml",e);
        }
    }

    /**
     * 通过Path创建来自任何文件系统的可读写的配置。
     *
     * @param path
     */
    public XmlConfig(Path path) {
        this.path = path;
        property = new HashMap<>();
        children = new HashMap<>();
        SAXReader reader = new SAXReader();
        try (InputStream in = Files.newInputStream(path)){
            document = reader.read(in);
            Element root = document.getRootElement();
            List<Element> elements = root.elements();
            for (Element element: elements) {
                if (element.elements() != null && element.elements().size() > 0) {
                    XmlConfig config = new XmlConfig(element);
                    config.setParent(this);
                    this.children.put(element.attributeValue("key"),config);
                } else {
                    String key = element.attributeValue("key");
                    String val = element.getText();
                    property.put(key,val);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("无法读取xml",e);
        }
    }

    /**
     * 通过文件创建可读写的Xml配置。
     * @param file
     */
    public XmlConfig(File file) {
        this.file = file;
        property = new HashMap<>();
        children = new HashMap<>();
        SAXReader reader = new SAXReader();
        try {
            document = reader.read(file);
            Element root = document.getRootElement();
            List<Element> elements = root.elements();
            for (Element element: elements) {
                if (element.elements() != null && element.elements().size() > 0) {
                    XmlConfig config = new XmlConfig(element);
                    config.setParent(this);
                    this.children.put(element.attributeValue("key"),config);
                } else {
                    String key = element.attributeValue("key");
                    String val = element.getText();
                    property.put(key,val);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("无法读取xml",e);
        }
    }

    /**
     * 子配置的创建方法，不公开。
     * @param element
     */
    private XmlConfig(Element element) {
        this.children = new HashMap<>();
        this.property = new HashMap<>();

        List<Element> elements = element.elements();
        this.content = element.getText().trim();
        for (Element elem: elements) {
            if (element.elements() != null && element.elements().size() > 0) {
                XmlConfig config = new XmlConfig(elem);
                config.setParent(this);
                children.put(elem.attributeValue("key"),config);
            }
        }

    }

    @Override
    protected void saveInternal() {
        if (file == null && path == null) {
            throw new RuntimeException("此配置是只读的，无法存储。");
        }
        Path target = file != null ? file.toPath(): path;
        try (OutputStream outputStream = Files.newOutputStream(target)){
            Element root = document.getRootElement();
            saveElement(this,root);
            XMLWriter writer = new XMLWriter(outputStream);
            writer.write(document);
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("无法写入配置文件。",e);
        }
    }

    private void saveElement(XmlConfig config, Element item) {
        List<Element> elements = item.elements("config");
        for (Element element: elements) {
            String key = element.attributeValue("key");
            if (config.property.containsKey(key)) {
                element.setText(config.property.get(key));
            }
            if (element.elements().size() > 0) {
                saveElement(config.children.get(key),element);
            }
        }
    }

    @Override
    public Configure getConfig(String key) {
        if (key.contains(".")) {
            String childKey = key.substring(0,key.indexOf("."));
            XmlConfig next = this.children.get(childKey);
            String nextKey = key.substring(key.indexOf(".") + 1);
            return next.getConfig(nextKey);
        } else {
            return this.children.get(key);
        }
    }

    @Override
    public <T> T getConfig(String key, Class<T> clazz) {
        String result;
        if (key == null) {
            result = this.content;
        } else {
            if (key.contains(".")) {
                String childKey = key.substring(0,key.indexOf("."));
                XmlConfig next = this.children.get(childKey);
                result = next.getConfig(key.substring(key.indexOf(".") + 1),String.class);
            } else {
                result = this.property.get(key);
                if (result == null) {
                    result = this.children.get(key).content.trim();
                }
            }
        }
        if (clazz == String.class) {
            return (T)result;
        } else {
            Converters converters = getConverters();
            Converter converter = converters.getConverter(String.class,clazz);
            if (converter == null) {
                throw new RuntimeException("不认识的类型：" + clazz + " 请手动注册转换器。");
            }
            return (T)converter.convert(result);
        }
    }

    @Override
    public void setConfig(String key, Object value) {
        String val = null;
        if (value.getClass() == String.class) {
            val = (String)value;
        } else {
            Converter converter = this.getConverters().getConverter(value.getClass(),String.class);
            if (converter == null) {
                throw new RuntimeException("无法转换目标对象为String，请手动添加Converter");
            }
            val = (String) converter.convert(value);
        }
        if (key == null) {
            this.content = val;
            return;
        }
        if (key.contains(".")) {
            String childKey = key.substring(0,key.indexOf("."));
            XmlConfig next = this.children.get(childKey);
            next.setConfig(key.substring(key.indexOf(".") + 1),val);
        } else {
            this.property.put(key,val);
        }
    }
}
