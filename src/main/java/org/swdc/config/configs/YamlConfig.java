package org.swdc.config.configs;

import org.swdc.config.Configure;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class YamlConfig extends MapBasedConfig {

    private File file;
    private Yaml yaml;
    private Path path;

    /**
     * 通过InputStream创建只读的Yaml配置。
     * @param inputStream
     */
    public YamlConfig(InputStream inputStream) {
        try {
            this.yaml = new Yaml();
            this.properties = yaml.loadAs(inputStream, HashMap.class);
        } catch (Exception e) {
            throw new RuntimeException("无法加载Yaml配置文件",e);
        }
    }

    /**
     * 通过Path创建来自任何文件系统的Yaml配置。
     * 可以读写。
     * @param path
     */
    public YamlConfig(Path path) {
        this.path = path;
        try (InputStream inputStream = Files.newInputStream(path)) {
            this.yaml = new Yaml();
            this.properties = yaml.loadAs(inputStream, HashMap.class);
        } catch (Exception e) {
            throw new RuntimeException("无法加载Yaml配置文件",e);
        }
    }

    /**
     * 通过File创建Yaml的配置。
     * 可以读写
     * @param file
     */
    public YamlConfig(File file){
        this.file = file;
        this.yaml = new Yaml();
        try {
            this.properties = yaml.loadAs(new FileInputStream(file), HashMap.class);
        } catch (Exception e) {
            throw new RuntimeException("无法加载Yaml配置文件",e);
        }
    }

    /**
     * 子配置的构造方法，不公开。
     * @param prefix
     */
    private YamlConfig(String prefix) {
        this.prefix = prefix;
    }

    @Override
    protected void saveInternal() {
        if (file == null && path == null) {
            throw new RuntimeException("无法写入配置，此配置是只读的。");
        }
        Path target = file != null ? file.toPath() : path;
        try(OutputStream outputStream = Files.newOutputStream(target)) {
            yaml.dump(this.properties,new OutputStreamWriter(outputStream));
        } catch (Exception e) {
            throw new RuntimeException("无法存储Yaml配置文件。",e);
        }
    }

    @Override
    public Configure getConfig(String key) {
        if (this.getParent() != null) {
            return this.getParent().getConfig(prefix + "." + key);
        }
        if (isKeyExists(key)) {
            if (key.contains(".")) {
                YamlConfig config = new YamlConfig(key);
                config.setParent(this);
                return config;
            } else {
                if (properties.containsKey(key) && Map.class.isAssignableFrom(properties.get(key).getClass())) {
                    YamlConfig config = new YamlConfig(key);
                    config.setParent(this);
                    return config;
                }
            }
        }
        return null;
    }

}
