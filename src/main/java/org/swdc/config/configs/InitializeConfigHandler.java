package org.swdc.config.configs;

import org.swdc.config.AbstractConfig;
import org.swdc.config.ConfigHandler;
import org.swdc.config.Converter;
import org.swdc.config.Reflections;
import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initialization (Ini)类型的配置文件的处理。
 */
public class InitializeConfigHandler <T extends AbstractConfig> implements ConfigHandler<T> {

    private static Map<Class, Map<Property, Field>> propertiesMap = new HashMap<>();
    private ConfigureSource source;

    @Override
    public void save(T configObj) throws IOException {

        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }

        if (!this.source.external()) {
            return;
        }

        Map<Property,Field> refs = this.getReflection(configObj.getClass());

        Map<String,Map<String,String>> data = new HashMap<>();
        for (Map.Entry<Property,Field> entry: refs.entrySet()) {
            String[] keys = entry.getKey().value().split("[.]");
            Field target = entry.getValue();
            if (keys.length > 2) {
                throw new RuntimeException("配置的格式错误。" + target.getName());
            }
            try {
                if (!Reflections.isSystemType(target.getType()) && keys.length == 1) {
                    Map<String,String> value = writeSectionObject(target.get(configObj));
                    data.put(keys[0],value);
                } else {
                    String section = keys[0];
                    String sectionKey = keys[1];

                    Map<String,String> sectionData = null;
                    if (data.containsKey(section)) {
                        sectionData = data.get(section);
                    } else {
                        sectionData = new HashMap<>();
                        data.put(section,sectionData);
                    }

                    Object val = target.get(configObj);
                    if (target.getType().equals(String.class)) {
                        sectionData.put(sectionKey,(String) val);
                    } else {

                        if (List.class.isAssignableFrom(target.getType())) {
                            val = this.writeList((List) val);
                            sectionData.put(sectionKey,(String) val);
                            continue;
                        }

                        Converter conv = converters.getConverter(target.getType(),String.class);
                        if (conv == null) {
                            throw new RuntimeException("无法转换类型： String to " + target.getType());
                        }
                        String realValue = (String) conv.convert(val);
                        sectionData.put(sectionKey,realValue);
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }

        // 构建ini文本
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,Map<String,String>>  entry: data.entrySet()) {
            String sec = "[" + entry.getKey() + "]\n";
            sb.append(sec);
            for (Map.Entry<String,String>  entVal: entry.getValue().entrySet()) {
                sb.append(entVal.getKey())
                        .append(" = ")
                        .append(entVal.getValue())
                        .append("\n");
            }

            sb.append("\n");
        }

        OutputStream out = this.getOutputStream(source);
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));

    }

    private Map<String,String> writeSectionObject(Object obj) {
        Map<String,String> sectionData = new HashMap<>();
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field: fields) {
            try {
                field.setAccessible(true);
                Object data  = field.get(obj);
                if (Reflections.isSystemType(data.getClass())) {
                    sectionData.put(field.getName(),data.toString());
                } else if (List.class.isAssignableFrom(data.getClass())) {
                    String val = writeList((List) data);
                    sectionData.put(field.getName(),val);
                }
            } catch (Exception e) {
                continue;
            }
        }
        return sectionData;
    }

    private String writeList(List data) {
        StringBuilder builder = new StringBuilder();
        for (Object item: data) {
            builder.append(",").append(item);
        }
        return builder.substring(1,builder.length());
    }

    @Override
    public void load(T configObj) throws IOException {

        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }
        try {
            Map<String,Map<String,String>> data = new HashMap<>();

            //  解析Ini的文件

            InputStream in = this.getInputStream(source);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line = null;
            Map<String,String> sectionContent = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(";") || line.isEmpty()) {
                    // 注释或空行
                    continue;
                }

                line = line.replaceAll(" ","");

                if (line.startsWith("[") && line.endsWith("]")) {
                    // section
                    String sectionName = line.substring(1,line.length() - 1);
                    if (data.containsKey(sectionName)) {
                        sectionContent = data.get(sectionName);
                    } else {
                        sectionContent = new HashMap<>();
                        data.put(sectionName,sectionContent);
                    }
                } else {
                    if (sectionContent == null) {
                        // 严重错误，缺少section。
                        throw new RuntimeException("ini文件格式有错误，缺少section。");
                    }
                    if (!line.contains("=")) {
                        continue;
                    }
                    String[] entry = line.split("=");
                    if (entry.length > 2) {
                        continue;
                    }
                    sectionContent.put(entry[0],entry[1]);
                }
            }


            // ini 文件解析完毕
            // 加载配置到Config对象。

            Map<Property,Field> reflection = this.getReflection(configObj.getClass());
            for (Map.Entry<Property,Field> ent: reflection.entrySet()) {
                String[] key = ent.getKey().value().split("[.]");
                Field target = ent.getValue();
                target.setAccessible(true);
                try {
                    if (key.length > 2) {
                        throw new RuntimeException("格式错误。");
                    } else if (!Reflections.isSystemType(target.getType()) && key.length == 1) {
                        // 是Object类型，这个是一整个section直接映射到Object
                        Object sectionObj = this.readSection(target,data.get(key[0]));
                        target.set(configObj,sectionObj);
                    } else {
                        String section = key[0];
                        String sectionKey = key[1];

                        Map<String,String> sec = data.get(section.trim());
                        if (sec == null) {
                            continue;
                        }
                        String value = sec.get(sectionKey.trim());
                        if (target.getType().equals(String.class)) {
                            target.set(configObj,value);
                        } else {
                            if (List.class.isAssignableFrom(target.getType())) {
                                List list = this.readList(target,value);
                                target.set(configObj,list);
                            } else {
                                Converter conv = converters.getConverter(String.class,target.getType());
                                if (conv == null) {
                                    throw new RuntimeException("无法转换类型： String to " + target.getType());
                                }
                                Object realValue = conv.convert(value);
                                target.set(configObj,realValue);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object readSection(Field field, Map<String,String> content) {
        Class type = field.getType();
        Field[] fields = type.getDeclaredFields();

        try {
            Object instance = type.getConstructor().newInstance();
            for (Field item: fields) {
                item.setAccessible(true);
                String name = item.getName();
                if (!content.containsKey(name)) {
                    continue;
                }
                String val = content.get(name);
                if (item.getType().equals(String.class)) {
                    item.set(instance,val);
                } else {
                    if (item.getType().isAssignableFrom(List.class)) {
                        List data = this.readList(item,val);
                        item.set(instance,data);
                    } else {
                        Converter conv = converters.getConverter(String.class,item.getType());
                        if (conv == null) {
                            throw new RuntimeException("无法转换类型： String to " + item.getType());
                        }
                        Object realValue = conv.convert(val);
                        item.set(instance,realValue);
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List readList(Field field,String value) {
        String[] items = value.split(",");

        ParameterizedType paramType = (ParameterizedType) field.getGenericType();
        Class real = (Class) paramType.getActualTypeArguments()[0];
        List list = new ArrayList();

        for (String obj: items) {
            if (obj.getClass() != real) {
                Converter conv = converters.getConverter(obj.getClass(),real);
                if (conv == null) {
                    throw new RuntimeException("无法转换类型： String to " + field.getType());
                }
                Object realValue = conv.convert(obj);
                list.add(realValue);
            } else {
                list.add(obj);
            }
        }

        return list;
    }

    public Map<Property, Field> getReflection(Class clazz) {
        if (!propertiesMap.containsKey(clazz)) {
            Map<Property,Field> refs = ConfigHandler.super.getReflection(clazz);
            propertiesMap.put(clazz,refs);
        }
        return propertiesMap.get(clazz);
    }

}
