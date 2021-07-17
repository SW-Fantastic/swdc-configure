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

        Map<Property,Field> fieldMap = this.getReflection(configObj.getClass());


        for (Map.Entry<Property,Field>  ent: fieldMap.entrySet()) {
            try {
                String key = ent.getKey().value();
                Field field = ent.getValue();
                if (Reflections.isSystemType(field.getType())) {
                    Object value = field.get(configObj);
                    String property = "";
                    if (value.getClass() != String.class) {
                        Converter conv = converters.getConverter(field.getType(),String.class);
                        if (conv == null) {
                            throw new RuntimeException("无法转换类型： String to " + field.getType());
                        }
                        property = (String) conv.convert(value);
                    } else {
                        property = (String) field.get(configObj);
                    }
                    properties.setProperty(key,property);
                } else {
                    if (List.class.isAssignableFrom(field.getType())) {
                        String property = this.writeList(field,(List) field.get(configObj));
                        properties.setProperty(key,property);
                    } else {
                        this.setPropertiesObjects(key,field.get(configObj),properties);
                    }

                }
            }  catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        OutputStream outputStream = this.getOutputStream(source);
        properties.store(outputStream,"configuration");
    }

    private String writeList(Field field,List props) {
        ParameterizedType paramType = (ParameterizedType) field.getGenericType();
        Class real = (Class) paramType.getActualTypeArguments()[0];
        if (!Reflections.isSystemType(real)) {
            throw new RuntimeException("properties不支持List嵌套Object");

        }

        StringBuilder data = new StringBuilder();
        for (Object item: props) {
            data.append(item);
            data.append(",");
        }

        return data.substring(0,data.length() - 1);
    }

    @Override
    public void load(T configObj) throws IOException {
        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }
        InputStream in = this.getInputStream(source);
        Properties properties = new Properties();
        properties.load(in);

        Map<Property,Field> fieldMap = this.getReflection(configObj.getClass());


        for (Map.Entry<Property,Field>  ent: fieldMap.entrySet()) {
            try {
                String key = ent.getKey().value();
                Field field = ent.getValue();

                if (Reflections.isSystemType(field.getType()) && properties.containsKey(key)) {
                    field.setAccessible(true);
                    String value = properties.getProperty(key);
                    if (field.getType() == String.class) {
                        field.set(configObj,value);
                    } else {
                        Converter conv = converters.getConverter(String.class,field.getType());
                        if (conv == null) {
                            throw new RuntimeException("无法转换类型： String to " + field.getType());
                        }
                        Object realValue = conv.convert(field.getType());
                        field.set(configObj,realValue);
                    }
                } else {

                    String prefix = key;
                    Object value = this.loadPropertiesObject(prefix,field.getType(),properties);

                    if (List.class.isAssignableFrom(field.getType())) {
                        value = this.loadList(field,properties.getProperty(key));
                    }

                    field.set(configObj,value);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setPropertiesObjects(String prefix, Object obj, Properties properties) {
        try {
            Class type = obj.getClass();
            Field[] fields = type.getDeclaredFields();
            for (Field field: fields) {
                field.setAccessible(true);
                if (Reflections.isSystemType(field.getType())) {
                    Object value = field.get(obj);;
                    if (value.getClass() == String.class) {
                        properties.setProperty(prefix + "." + field.getName(), (String) value);
                    } else {
                        Converter conv = converters.getConverter(String.class,field.getType());
                        if (conv == null) {
                            throw new RuntimeException("无法转换类型： String to " + field.getType());
                        }
                        Object realValue = conv.convert(field.getType());;
                        properties.setProperty(prefix + "." + field.getName(), (String) realValue);
                    }
                } else {
                    if (List.class.isAssignableFrom(field.getType())) {
                        List data = (List) field.get(obj);
                        String value = this.writeList(field,data);
                        properties.setProperty(prefix + "." + field.getName(), value);
                    } else {
                        this.setPropertiesObjects(prefix + "." + field.getName(),field.getType(),properties);

                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private List loadList(Field field, String prop) {
        ParameterizedType paramType = (ParameterizedType) field.getGenericType();
        Class real = (Class) paramType.getActualTypeArguments()[0];
        if (!Reflections.isSystemType(real)) {
            throw new RuntimeException("properties不支持List嵌套Object");

        }
        String[] values = prop.split(",");
        ArrayList list = new ArrayList();

        for (String item: values) {
            if (real.equals(String.class)) {
                list.add(item);
            } else {
                Converter conv = converters.getConverter(item.getClass(),real);
                if (conv == null) {
                    throw new RuntimeException("无法转换类型： String to " + field.getType());
                }
                Object realValue = conv.convert(item);
                list.add(realValue);
            }
        }
        return list;
    }

    private Object loadPropertiesObject(String prefix,Class type, Properties properties) {
        try {
            Object target = type.getConstructor().newInstance();
            Field[] fields = type.getDeclaredFields();
            for (Field field: fields) {
                field.setAccessible(true);
                if (Reflections.isSystemType(field.getType())) {
                    String value = properties.getProperty(prefix + "." + field.getName());
                    if (field.getType() == String.class) {
                        field.set(target,value);
                    } else {
                        Converter conv = converters.getConverter(String.class,field.getType());
                        if (conv == null) {
                            throw new RuntimeException("无法转换类型： String to " + field.getType());
                        }
                        Object realValue = conv.convert(value);
                        field.set(target,realValue);
                    }
                } else {
                    if (List.class.isAssignableFrom(field.getType())){
                        // 是一个list
                        String val = properties.getProperty(prefix + "." + field.getName());
                        field.set(target,loadList(field,val));
                        continue;
                    }
                    Object value = this.loadPropertiesObject(prefix + "." + field.getName(),field.getType(),properties);
                    field.set(target,value);
                }
            }
            return target;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
