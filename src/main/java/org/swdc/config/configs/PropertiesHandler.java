package org.swdc.config.configs;

import org.swdc.config.AbstractConfig;
import org.swdc.config.ConfigHandler;
import org.swdc.config.Converter;
import org.swdc.config.Reflections;
import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * Properties类型的配置文件的处理。
 *
 * 也就是java的properties文件格式，你可以使用任何符合properties格式的
 * 配置条目。
 *
 * @param <T>
 */
public class PropertiesHandler  <T extends AbstractConfig> implements ConfigHandler<T> {

    private ConfigureSource source;

    @Override
    public void save(T configObj) throws IOException {
        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }
        if (!source.external()) {
            return;
        }
        Properties properties = new Properties();
        writeProperties(null,configObj,properties);
        OutputStream outputStream = this.getOutputStream(source);
        properties.store(outputStream,"configuration");
    }

    private void writeProperties(String thePrefix,Object configObj ,Properties properties) {

        Map<Property,Field> fieldMap = this.getReflection(configObj.getClass());

        for (Map.Entry<Property,Field>  ent: fieldMap.entrySet()) {
            try {
                String key = ent.getKey().value();
                if (thePrefix != null) {
                    key = thePrefix + "." + key;
                }

                Field field = ent.getValue();
                field.setAccessible(true);

                if (Reflections.isSystemType(field.getType())) {
                    Object value = field.get(configObj);

                    if (field.getType() == String.class) {
                        properties.setProperty(key,value.toString());
                    } else {
                        Converter conv = converters.getConverter(field.getType(),String.class);
                        if (conv == null) {
                            throw new RuntimeException("无法转换类型： String from " + field.getType());
                        }
                        Object realValue = conv.convert(value);
                        properties.setProperty(key,realValue.toString());
                    }
                } else if (Reflections.isCollectionType(field.getType())) {

                    if (Reflections.isList(field.getType())) {

                        List<Class> typeParams = Reflections.getFieldParameters(field);
                        if (typeParams.isEmpty()) {
                            continue;
                        }

                        Class realType = typeParams.get(0);
                        if (!Reflections.isSystemType(realType)) {
                            throw new RuntimeException("properties不支持在List中嵌套基本类型之外的对象。");
                        }

                        List values = (List) field.get(configObj);
                        String value = "";
                        for (Object item: values) {
                            if (realType == String.class) {
                                value = value.isBlank() ?
                                        item.toString() :
                                        value + "," + item.toString();
                            } else {
                                Converter converter = converters.getConverter(realType,String.class);
                                if (converter == null) {
                                    throw new RuntimeException("无法把String转换为：" + realType.getName());
                                }
                                String realValue = converter.convert(item).toString();
                                value = value.isBlank() ?
                                        realValue :
                                        value + "," + realValue;
                            }
                        }
                        properties.setProperty(key,value);
                    } else if (Reflections.isMap(field.getType())) {

                        List<Class> typeParams = Reflections.getFieldParameters(field);
                        if (typeParams.size() < 2 || !properties.containsKey(key)) {
                            continue;
                        }

                        Class keyType = typeParams.get(0);
                        Class valType = typeParams.get(1);
                        if (!Reflections.isSystemType(keyType) || !Reflections.isSystemType(valType)) {
                            throw new RuntimeException("properties不支持在Map中嵌套基本类型之外的对象。");
                        }

                        Map<Object,Object> values = (Map) field.get(configObj);
                        StringBuilder pairs = new StringBuilder();

                        for (Map.Entry pair : values.entrySet()) {

                            String theKey = null;
                            String value = null;

                            if (keyType == String.class) {
                                theKey = pair.getKey().toString();
                            } else {
                                Converter converter = converters.getConverter(keyType,String.class);
                                if (converter == null) {
                                    throw new RuntimeException("无法把String转换为：" + keyType.getName());
                                }
                                theKey = converter.convert(pair.getKey()).toString();
                            }

                            if (valType == String.class) {
                                value = pair.getValue().toString();
                            } else {
                                Converter converter = converters.getConverter(valType,String.class);
                                if (converter == null) {
                                    throw new RuntimeException("无法把String转换为：" + valType.getName());
                                }
                                value = converter.convert(pair.getValue()).toString();
                            }
                            if (pairs.length() > 0) {
                                pairs.append(",");
                            }
                            pairs.append(theKey).append("=").append(value);
                        }
                        properties.setProperty(key,pairs.toString());
                    }

                } else {

                    Object subConf = field.get(configObj);
                    writeProperties(key,subConf,properties);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void load(T configObj) throws IOException {
        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }
        InputStream in = this.getInputStream(source);
        Properties properties = new Properties();
        properties.load(in);

        readPropertiesConfig(null,configObj,properties);
    }

    private void readPropertiesConfig(String thePrefix,Object configObj, Properties properties) {
        Map<Property,Field> fieldMap = this.getReflection(configObj.getClass());

        for (Map.Entry<Property,Field>  ent: fieldMap.entrySet()) {
            try {
                String key = ent.getKey().value();
                if (thePrefix != null) {
                    key = thePrefix + "." + key;
                }

                Field field = ent.getValue();
                field.setAccessible(true);

                if (Reflections.isSystemType(field.getType())) {
                    if (!properties.containsKey(key)){
                        continue;
                    }
                    String value = properties.getProperty(key);
                    if (field.getType() == String.class) {
                        field.set(configObj,value);
                    } else {
                        Converter conv = converters.getConverter(String.class,field.getType());
                        if (conv == null) {
                            throw new RuntimeException("无法转换类型： String to " + field.getType());
                        }
                        Object realValue = conv.convert(value);
                        field.set(configObj,realValue);
                    }
                } else if (Reflections.isCollectionType(field.getType())) {

                    if (Reflections.isList(field.getType())) {

                        List<Class> typeParams = Reflections.getFieldParameters(field);
                        if (typeParams.isEmpty() || !properties.containsKey(key)) {
                            continue;
                        }

                        Class realType = typeParams.get(0);
                        if (!Reflections.isSystemType(realType)) {
                            throw new RuntimeException("properties不支持在List中嵌套基本类型之外的对象。");
                        }

                        List value = new ArrayList();
                        String[] values = properties.getProperty(key).split(",");
                        for (String item: values) {
                            if (realType == String.class) {
                                value.add(item);
                            } else {
                                Converter converter = converters.getConverter(String.class,realType);
                                if (converter == null) {
                                    throw new RuntimeException("无法把String转换为：" + realType.getName());
                                }
                                value.add(converter.convert(item));
                            }
                        }
                        field.set(configObj,value);
                    } else if (Reflections.isMap(field.getType())) {

                        List<Class> typeParams = Reflections.getFieldParameters(field);
                        if (typeParams.size() < 2 || !properties.containsKey(key)) {
                            continue;
                        }

                        Class keyType = typeParams.get(0);
                        Class valType = typeParams.get(1);
                        if (!Reflections.isSystemType(keyType) || !Reflections.isSystemType(valType)) {
                            throw new RuntimeException("properties不支持在Map中嵌套基本类型之外的对象。");
                        }

                        Map resolved = new HashMap();
                        String[] pairs = properties.getProperty(key).split(",");
                        for (String pair : pairs) {
                            if (!pair.contains("=") || pair.startsWith("=") || pair.endsWith("=")) {
                                continue;
                            }
                            Object theKey;
                            Object value;
                            String[] kv = pair.split("=");
                            if (kv.length != 2) {
                                continue;
                            }
                            if (keyType == String.class) {
                                theKey = kv[0];
                            } else {
                                Converter converter = converters.getConverter(String.class,keyType);
                                if (converter == null) {
                                    throw new RuntimeException("无法把String转换为：" + keyType.getName());
                                }
                                theKey = converter.convert(kv[0]);
                            }

                            if (valType == String.class) {
                                value = kv[1];
                            } else {
                                Converter converter = converters.getConverter(String.class,valType);
                                if (converter == null) {
                                    throw new RuntimeException("无法把String转换为：" + valType.getName());
                                }
                                value = converter.convert(kv[1]);
                            }

                            resolved.put(theKey,value);
                        }
                        field.set(configObj,resolved);
                    }

                } else {

                    Object subConf = field.getType().getConstructor().newInstance();
                    readPropertiesConfig(key,subConf,properties);

                    field.set(configObj,subConf);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <T> T unManaged(Class<T> target, Properties properties) {
        try {
            Object conf = target.getConstructor()
                    .newInstance();
            PropertiesHandler handler = new PropertiesHandler();
            handler.readPropertiesConfig(null,conf,properties);
            return (T)conf;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
