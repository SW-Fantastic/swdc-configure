package org.swdc.config.configs;

import org.swdc.config.AbstractConfig;
import org.swdc.config.ConfigHandler;
import org.swdc.config.Converter;
import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Yaml类型的文件的处理。
 * @param <T>
 */
public class YamlConfigHandler  <T extends AbstractConfig> implements ConfigHandler<T> {

    private static Map<Class, Map<Property, Field>> propertiesMap = new HashMap<>();
    private ConfigureSource source;
    private Yaml properties = new Yaml();


    @Override
    public void save(T configObj) throws IOException {
        if (source != null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }
        if (!source.external()) {
            return;
        }

        Map<String,Object> result = new HashMap<>();
        Map<Property,Field> refMap = this.getReflection(configObj.getClass());
        for (Map.Entry<Property,Field> ent: refMap.entrySet()) {
            try {
                String key = ent.getKey().value();
                Field field = ent.getValue();

                String[] paths = key.split("[.]");
                Map<String,Object> content = result;
                for (int idx = 0;idx < paths.length - 1; idx ++) {
                    if (!content.containsKey(paths[idx])){
                        Map<String,Object>  next = new HashMap<>();
                        content.put(paths[idx],next);
                        content = next;
                    } else {
                        content = (Map<String, Object>) content.get(paths[idx]);
                    }
                }

                // 无论类型直接写入，yaml会自动标注类型。
                Object value = field.get(configObj);
                content.put(paths[paths.length - 1], value);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        OutputStream out = this.getOutputStream(source);
        out.write(this.properties.dump(result).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void load(T configObj) throws IOException {
        if (source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }
        InputStream in = this.getInputStream(source);
        if (source.external()) {
            File configFile = new File(source.value());
            configObj.setSourceFile(configFile);
        }

        Map<String,Object> data = this.properties.load(in);
        this.reverseLoad(data,null,null);

        Map<Property,Field> refMap = this.getReflection(configObj.getClass());
        for (Map.Entry<Property,Field> ent: refMap.entrySet()) {

            try {

                String key = ent.getKey().value();
                Field field = ent.getValue();
                field.setAccessible(true);

                String[] paths = key.split("[.]");
                Map<String,Object> content = data;
                for (int idx = 0;idx < paths.length - 1; idx ++) {
                    content = (Map<String, Object>) content.get(paths[idx]);
                }
                Object value = content.get(paths[paths.length - 1]);
                // 对于Config中自定义的类和对象，yaml根据标注的类型自动转换
                // 但是需要在配置中标注类的全限定名。
                if (value.getClass() == field.getType()) {
                    field.set(configObj,value);
                } else {
                    Converter conv = converters.getConverter(value.getClass(),field.getType());
                    if (conv == null) {
                        throw new RuntimeException("无法转换类型： String to " + field.getType());
                    }
                    Object realValue = conv.convert(value);
                    field.set(configObj,realValue);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }


    private Map<String,Object> reverseLoad(Map<String,Object> originalData,Map<String,Object> container, String key) {
        if (originalData == null) {
            return new HashMap<>();
        }
        if (container == null) {
            container = new HashMap<>();
        }
        for (Map.Entry<String,Object> ent: originalData.entrySet()) {
            String entryKey = ent.getKey();
            Object value = ent.getValue();
            if (Map.class.isAssignableFrom(value.getClass())) {
                reverseLoad((Map<String, Object>) value,container,key + "." + entryKey);
            } else {
                container.put(key == null || key.isBlank() ?
                        entryKey :
                        key + "." + entryKey,value);
            }
        }
        return container;
    }

    @Override
    public Map<Property, Field> getReflection(Class clazz) {
        if (!propertiesMap.containsKey(clazz)) {
            Map<Property,Field> refs = ConfigHandler.super.getReflection(clazz);
            propertiesMap.put(clazz,refs);
        }
        return propertiesMap.get(clazz);
    }
}
