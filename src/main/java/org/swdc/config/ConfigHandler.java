package org.swdc.config;

import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;
import org.swdc.config.converters.Converters;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 这个是处理Config的接口。
 */
public interface ConfigHandler<T> {

    Map<Class, Map<Property, Field>> propertiesMap = new HashMap<>();

    /**
     * 如果使用了FileSystem，我们需要在流关闭的时候同时
     * 关闭FileSystem。
     */
    class FSAutoCloseInputStream extends FilterInputStream {

        private FileSystem fs;

        public FSAutoCloseInputStream(InputStream in, FileSystem fs) {
            super(in);
            this.fs = fs;
        }

        @Override
        public void close() throws IOException {
            super.close();
            fs.close();
        }
    }

    /**
     * 如果使用了FileSystem，我们需要在流关闭的时候同时
     * 关闭FileSystem。
     */
    class FSAutoCloseOutputStream extends FilterOutputStream {

        private FileSystem fs;

        public FSAutoCloseOutputStream(OutputStream out, FileSystem fs) {
            super(out);
            this.fs = fs;
        }

        @Override
        public void close() throws IOException {
            super.close();
            fs.close();
        }
    }

    Converters converters = new Converters();

    /**
     * 保存配置到文件。
     * @param configObj
     */
    void save(T configObj) throws IOException;

    /**
     * 载入config的内容到config对象。
     * @param configObj
     */
    void load(T configObj) throws IOException;

    default Map<Property, Field> getReflection(Class clazz) {

        if (propertiesMap.containsKey(clazz)) {
            return propertiesMap.get(clazz);
        }

        Map<Property,Field> result = new HashMap<>();

        Class target = clazz;

        while (target != null && target != AbstractConfig.class){
            Field[] fields = target.getDeclaredFields();
            for (Field field: fields) {
                Property property = field.getAnnotation(Property.class);
                if (property == null) {
                    continue;
                }
                field.setAccessible(true);
                result.put(property,field);
            }
            target = target.getSuperclass();
        }

        return result;
    }

    default InputStream getInputStream(ConfigureSource source) throws IOException {
        InputStream in = null;
        if (source.filesystem().isEmpty()) {
            if (source.external()) {
                File configFile = new File(source.value());
                in = new FileInputStream(configFile);
            } else {
                in = source.loadForm().getModule().getResourceAsStream(source.value());
            }
        } else {
            String fsUrl = source.filesystem();
            URI uri = URI.create(fsUrl);
            FileSystem fs = FileSystems.newFileSystem(uri,new HashMap<>());
            String path = uri.getPath();
            Path target = fs.getPath(path);
            in = new FSAutoCloseInputStream(Files.newInputStream(target),fs);
        }

        return in;
    }

    default OutputStream getOutputStream(ConfigureSource source) throws IOException {
        OutputStream out = null;
        if (source.filesystem().isEmpty()) {
            if (!source.external()) {
                return null;
            } else {
                out = new FileOutputStream(source.value());
            }
        } else {
            String fsUrl = source.filesystem();
            URI uri = URI.create(fsUrl);
            FileSystem fs = FileSystems.newFileSystem(uri,new HashMap<>());
            String path = uri.getPath();
            Path target = fs.getPath(path);
            out = new FSAutoCloseOutputStream(Files.newOutputStream(target),fs);
        }

        return out;
    }

    default Map<Property,Field> getConfigureInfo(Class clazz) {
        return Collections.unmodifiableMap(this.getReflection(clazz));
    }


}
