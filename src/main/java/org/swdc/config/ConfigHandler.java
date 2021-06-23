package org.swdc.config;

import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;
import org.swdc.config.converters.Converters;

import java.io.*;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

/**
 * 这个是处理Config的接口。
 */
public interface ConfigHandler<T> {

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
        if (source.external()) {
            File configFile = new File(source.value());
            in = new FileInputStream(configFile);
        } else {
            in = source.loadForm().getModule().getResourceAsStream(source.value());
        }
        return in;
    }

    default OutputStream getOutputStream(ConfigureSource source) throws IOException {
        if (!source.external()) {
            return null;
        }
        OutputStream out = new FileOutputStream(source.value());
        return out;
    }


}
