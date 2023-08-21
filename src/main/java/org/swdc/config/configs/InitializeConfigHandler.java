package org.swdc.config.configs;

import org.swdc.config.AbstractConfig;
import org.swdc.config.ConfigHandler;
import org.swdc.config.Reflections;
import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;
import org.swdc.ours.common.type.ClassTypeAndMethods;
import org.swdc.ours.common.type.Converter;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Initialization (Ini)类型的配置文件的处理。
 *
 * 关于Ini文件格式的读写，ini文件是一个或者多个section组成的，
 * 每一个section包含一个或多个properties条目的配置文件。
 *
 * section单独占用一行文本，在这一行的开始和结束的位置一对中括号，
 * 中括号内是section名称，例如：
 *
 * [section_name]
 *
 * section行之下就是属于此section的配置条目，他可以使用类似properties的格式，
 * 也就是键值对的形式编写，就像这样：
 *
 * [section_name]
 * property.name=name
 *
 * 对于Ini的配置类，应该遵守如下规则：
 * Property注解，应当通过“点“隔开section名称和内部的配置名称，
 * 对于上述示例中的property.name，应该在注解中写为“section_name.property.name"。
 * 也就是这样：
 * <code>
 *  @ConfigureSource(value = "your_config.ini",handler = InitializeConfigHandler.class)
 *  class YourConfig extend AbstractConfig {
 *
 *      @Property("section_name.property.name")
 *      private String sectionPropertyName;
 *      // getter and setter.
 *
 *  }
 * </code>
 *
 * 如果你为某一个section准备了单独的Class，希望直接把section作为特定的Object进行读写，
 * 你可以直接把字段声明为你自定义的类型，并且利用Property注解在此类型的字段上进行标注。
 *
 */
public class InitializeConfigHandler <T extends AbstractConfig> implements ConfigHandler<T> {

    private ConfigureSource source;

    @Override
    public void save(T configObj) throws IOException {

        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }

        if (!this.source.external()) {
            return;
        }

        Map<String,Map<String,String>> data = writeSection(null,configObj);

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

    private Map<String,Map<String,String>> writeSection(String theSection,Object configObj) {
        Map<Property,Field> reflection = this.getReflection(configObj.getClass());
        Map<String,Map<String,String>> data = new HashMap<>();
        for (Map.Entry<Property,Field> ent: reflection.entrySet()) {

            Field target = ent.getValue();
            target.setAccessible(true);
            try {
                String originKey = ent.getKey().value();
                if (!ClassTypeAndMethods.isCollectionType(target.getType()) && !Reflections.isSystemType(target.getType())) {
                    // 是Object类型，这个是一整个section直接映射到Object
                    data.putAll(writeSection(originKey,target.get(configObj)));
                } else {
                    String section = null;
                    String sectionKey = null;

                    if (theSection != null) {
                        section = theSection;
                        sectionKey = originKey;
                    } else {
                        if (!ent.getKey().value().contains(".")) {
                            throw new RuntimeException("Ini格式的每一个条目必须使用“.”分隔section和section的内容。");
                        }
                        int posDot = originKey.indexOf(".");
                        section = originKey.substring(0,posDot);
                        sectionKey = originKey.substring(posDot + 1);
                    }

                    Map<String,String> sec = data.computeIfAbsent(section,s -> new HashMap<>());
                    Object value = target.get(configObj);
                    if (target.getType().equals(String.class)) {
                        sec.put(sectionKey,value.toString());
                    } else {
                        if (List.class.isAssignableFrom(target.getType())) {
                            String list = this.writeList((List) value);
                            sec.put(sectionKey,list);
                        } else {
                            Converter conv = converters.getConverter(target.getType(),String.class);
                            if (conv == null) {
                                throw new RuntimeException("无法转换类型： String from " + target.getType());
                            }
                            Object realValue = conv.convert(value);
                            sec.put(sectionKey,realValue.toString());
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return data;
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
            readSection(null,configObj,data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readSection(String theSection,Object configObj, Map<String,Map<String,String>> data) {
        Map<Property,Field> reflection = this.getReflection(configObj.getClass());
        for (Map.Entry<Property,Field> ent: reflection.entrySet()) {

            Field target = ent.getValue();
            target.setAccessible(true);
            try {
                String originKey = ent.getKey().value();
                if (!ClassTypeAndMethods.isCollectionType(target.getType()) && !Reflections.isSystemType(target.getType())) {
                    // 是Object类型，这个是一整个section直接映射到Object
                    Object sectionObj = target.getType()
                            .getConstructor()
                            .newInstance();
                    this.readSection(originKey,sectionObj,data);
                    target.set(configObj,sectionObj);
                } else {
                    String section = null;
                    String sectionKey = null;

                    if (theSection != null) {
                        section = theSection;
                        sectionKey = originKey;
                    } else {
                        if (!ent.getKey().value().contains(".")) {
                            throw new RuntimeException("Ini格式的每一个条目必须使用“.”分隔section和section的内容。");
                        }
                        int posDot = originKey.indexOf(".");
                        section = originKey.substring(0,posDot);
                        sectionKey = originKey.substring(posDot + 1);
                    }

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
                throw new RuntimeException(e);
            }
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

}
