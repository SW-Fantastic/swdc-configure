package org.swdc.config.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.swdc.config.Configure;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Json的配置文件对象。
 */
public class JsonConfig extends MapBasedConfig {

    private File file;
    private Path path;
    private ObjectMapper mapper;

    /**
     * 通过inputStream创建，
     * 配置将会是只读的，无法通过Save存储。
     * @param stream 输入流
     */
    public JsonConfig(InputStream stream) {
        this.mapper = new ObjectMapper();
        try {
            this.properties = mapper.readValue(stream, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("无法读取Json配置");
        }
    }

    /**
     * 通过Path创建，Path可以来自各种Nio的
     * FileSystem，可以读写。
     * @param path
     */
    public JsonConfig(Path path) {
        this.path = path;
        this.mapper = new ObjectMapper();
        try (InputStream in = Files.newInputStream(path)){
            this.properties = mapper.readValue(in, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("无法读取Json配置");
        }
    }

    /**
     * 通过File创建，可以读写的配置对象。
     * @param file
     */
    public JsonConfig(File file) {
        this.file = file;
        this.mapper = new ObjectMapper();
        try {
            this.properties = mapper.readValue(file, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("无法读取Json配置");
        }
    }

    private JsonConfig(String prefix) {
        this.prefix = prefix;
    }

    @Override
    protected void saveInternal() {
        if (file == null && path == null) {
            throw new RuntimeException("此配置是只读的，无法存储。");
        }
        Path target = file != null ? file.toPath() : path;
        try {
            String data = mapper.writeValueAsString(this.properties);
            Files.writeString(target,data);
        } catch (Exception e){
            throw new RuntimeException("无法写入文件",e);
        }
    }

    @Override
    public Configure getConfig(String key) {
        if (this.getParent() != null) {
            return this.getParent().getConfig(prefix + "." + key);
        }
        if (isKeyExists(key)) {
            if (key.contains(".")) {
                JsonConfig config = new JsonConfig(key);
                config.setParent(this);
                return config;
            } else {
                if (properties.containsKey(key) && Map.class.isAssignableFrom(properties.get(key).getClass())) {
                    JsonConfig config = new JsonConfig(key);
                    config.setParent(this);
                    return config;
                }
            }
        }
        return null;
    }

}
