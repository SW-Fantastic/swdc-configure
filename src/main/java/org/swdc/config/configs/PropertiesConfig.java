package org.swdc.config.configs;

import org.swdc.config.AbstractConfig;
import org.swdc.config.Configure;
import org.swdc.config.Converter;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 处理Properties文件的配置类。
 */
public class PropertiesConfig extends AbstractConfig {

    private String prefix;

    private Map<String,String> configMap;
    private Map<String,PropertiesConfig> subConfigs;
    private File file;
    private Path path;

    /**
     * 通过inputStream创建只读的配置。
     * @param inputStream
     */
    public PropertiesConfig(InputStream inputStream) {
        configMap = new HashMap<>();
        subConfigs = new HashMap<>();

        Properties properties = new Properties();
        try {
            properties.load(inputStream);
            for (String key: properties.stringPropertyNames()) {
                configMap.put(key,properties.getProperty(key));
            }
        } catch (Exception e) {
            throw new RuntimeException("无法读取配置文件。",e);
        }
    }

    /**
     * 通过Path创建来自任何FileSystem的配置。
     * 可以读写
     * @param path
     */
    public PropertiesConfig(Path path) {
        this.path = path;
        configMap = new HashMap<>();
        subConfigs = new HashMap<>();

        Properties properties = new Properties();
        try(InputStream in = Files.newInputStream(path)) {
            properties.load(in);
            for (String key: properties.stringPropertyNames()) {
                configMap.put(key,properties.getProperty(key));
            }
        } catch (Exception e) {
            throw new RuntimeException("无法读取配置文件。",e);
        }
    }

    /**
     * 通过File创建配置。
     * 可以读写。
     * @param propertiesFile
     */
    public PropertiesConfig(File propertiesFile) {
        this.file = propertiesFile;
        configMap = new HashMap<>();
        subConfigs = new HashMap<>();

        Properties properties = new Properties();
        try(InputStream in = Files.newInputStream(propertiesFile.toPath())) {
            properties.load(in);
            for (String key: properties.stringPropertyNames()) {
                configMap.put(key,properties.getProperty(key));
            }
        } catch (Exception e) {
            throw new RuntimeException("无法读取配置文件。",e);
        }
    }

    /**
     * 子配置，不公开的构造方法。
     * @param prefix
     * @param parent
     */
    private PropertiesConfig(String prefix, PropertiesConfig parent) {
        setParent(parent);
        this.prefix = prefix;
    }

    @Override
    protected void saveInternal() {
        if (file == null && path == null) {
            throw new RuntimeException("只读的配置，不允许修改。");
        }
        Path target = file != null ? file.toPath() : path;
        Properties properties = new Properties();
        properties.putAll(this.configMap);
        try (OutputStream out = Files.newOutputStream(target)) {
            properties.store(out,"");
        } catch (Exception e) {
            throw new RuntimeException("无法存储配置",e);
        }
    }

    @Override
    public Configure getConfig(String key) {

        if (subConfigs.containsKey(key)) {
            return subConfigs.get(key);
        }

        boolean subConfExist = false;
        for (String keys: configMap.keySet()) {
            if (keys.contains(key) && !keys.equals(key)) {
                subConfExist = true;
            }
        }

        if (!subConfExist) {
            return null;
        }

        PropertiesConfig subConf = new PropertiesConfig(key,this);
        this.subConfigs.put(key,subConf);
        return subConf;
    }

    @Override
    public <T> T getConfig(String key, Class<T> clazz) {
        if (prefix != null) {
            key = prefix + "." + key;
        }
        if (this.getParent() != null) {
            return getParent().getConfig(key,clazz);
        }

        if (clazz == String.class) {
            return (T)configMap.get(key);
        } else if (clazz == Configure.class) {
            return (T)getConfig(key);
        }

        Converter<String, T> converter = getConverters().getConverter(String.class,clazz);
        if (converter != null && configMap.containsKey(key)) {
            return converter.convert(configMap.get(key));
        }
        return null;
    }

    @Override
    public void setConfig(String key, Object value) {
        if (prefix != null) {
            key = prefix + "." + key;
        }
        if (getParent() != null) {
            getParent().setConfig(key,value);
            return;
        }
        if (value.getClass() != String.class) {
            Converter converter = getConverters().getConverter(value.getClass(),String.class);
            if (converter == null) {
                throw new RuntimeException("不认识的类型，无法转换为String，请添加能够将该类型转换为String的Converter。");
            }
            configMap.put(key,converter.convert(value).toString());
            return;
        }
        configMap.put(key,value.toString());
    }

}
