package org.swdc.config.configs;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
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
import java.util.*;

/**
 * Xml类型的配置文件的处理。
 * @param <T>
 */
public class XmlConfigHandler<T extends AbstractConfig> implements ConfigHandler<T> {

    private static Map<Class, Map<Property, Field>> propertiesMap = new HashMap<>();
    private ConfigureSource source;

    @Override
    public void save(T configObj) throws IOException {
        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }
        if (!source.external()) {
            return;
        }
        OutputStream output = this.getOutputStream(source);

        Map<String,Object> result = new HashMap<>();
        Map<Property,Field> props = this.getReflection(configObj.getClass());
        for (Map.Entry<Property,Field> ent: props.entrySet()) {
            Field field = ent.getValue();
            Property property = ent.getKey();
            String key = property.value();
            String[] keyPaths = key.split("[.]");

            Map<String,Object> data = result;

            for (int idx = 0; idx < keyPaths.length - 1; idx ++) {
                String keyPart = keyPaths[idx];
                if (!data.containsKey(keyPart)) {
                    Map<String, Object> content = new HashMap<>();
                    data.put(keyPart, content);
                    data = content;
                } else {
                    data = (Map<String, Object>) data.get(keyPart);
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(configObj);
                    data.put(keyPaths[keyPaths.length - 1], value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        XStream xmlStream = new XStream();
        xmlStream.registerConverter(new MapEntryConverter());
        xmlStream.alias("configure",Map.class);
        output.write(xmlStream.toXML(result).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void load(T configObj) throws IOException {
        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }
        InputStream in = this.getInputStream(source);
        if (source.external()) {
            File configFile = new File(source.value());
            configObj.setSourceFile(configFile);
        }

        try {
            XStream xmlStream = new XStream();
            xmlStream.registerConverter(new MapEntryConverter());
            xmlStream.alias("configure",Map.class);
            // 将xml读取为map

            Map<String,Object> map = (Map<String, Object>) xmlStream.fromXML(in);

            Map<Property,Field> props = this.getReflection(configObj.getClass());
            for (Map.Entry<Property,Field> ent: props.entrySet()) {
                Field field = ent.getValue();

                Map<String,Object> data = map;
                Property property = ent.getKey();

                String key = property.value();
                String[] keyPaths = key.split("[.]");
                for (int idx = 0; idx < keyPaths.length - 1; idx ++) {
                    String keyPart = keyPaths[idx];
                    data = (Map<String, Object>) data.get(keyPart);
                }


                Object val = data.get(keyPaths[keyPaths.length - 1]);

                if (val instanceof Map) {
                    // 最终的值是一个map，这说明是一个嵌套在内部的对象
                    Class target = field.getType();
                    val = this.mapToObject((Map<String, Object>) val,target);
                } else if (val instanceof List) {
                    // 是一个list，转换类型使list内的对象类型和字段需要的泛型一致。
                    val = this.listToObject(field,val);
                }

                if (field.getType() == val.getClass()) {
                    field.set(configObj,val);
                } else {
                    Converter conv = converters.getConverter(val.getClass(),field.getType());
                    if (conv == null) {
                        throw new RuntimeException("无法转换类型： String to " + field.getType());
                    }
                    Object realValue = conv.convert(val);
                    field.set(configObj,realValue);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 转换list的内容，使其和字段的List的泛型一致。
     *
     * @param field
     * @param content
     * @return
     */
    private List listToObject(Field field,Object content) {
        ParameterizedType paramType = (ParameterizedType) field.getGenericType();
        Class real = (Class) paramType.getActualTypeArguments()[0];
        List list = new ArrayList();
        for (Object obj: (List)content) {
            if (obj.getClass() != real) {
                Converter conv = converters.getConverter(obj.getClass(),real);
                if (conv == null) {
                    throw new RuntimeException("无法转换类型： String to " + field.getType());
                }
                Object realValue = conv.convert(obj);
                list.add(realValue);
            } else {
                list.add(real);
            }
        }
        return list;
    }

    /**
     * 把map的内容映射到一个Object里面，
     * 就是说，map的key将会作为Object的field，
     * value则是作为Field的内容，这样生成一个
     * Java的POJO数据对象。
     * 
     * map的内容一般是基本的数据类型（参见Reflections的isSystemType)
     * @see Reflections#isSystemType(Class)
     *
     * @param data
     * @param target
     * @return
     */
    private Object mapToObject(Map<String,Object> data, Class target)  {
        Object result = null;
        try {
            result = target.getConstructor().newInstance();
            for (Map.Entry<String,Object> ent: data.entrySet()) {
                try {
                    Field field = target.getDeclaredField(ent.getKey());
                    Object content = ent.getValue();

                    field.setAccessible(true);

                    if (content instanceof Map) {
                        result = this.mapToObject((Map<String, Object>) content,field.getType());
                    } else if (content instanceof List) {
                        List list = listToObject(field,content);
                        field.set(result,list);
                        continue;
                    }

                    if (field.getType() == content.getClass())  {

                        field.set(result,content);

                    } else {
                        Converter conv = converters.getConverter(content.getClass(),field.getType());
                        if (conv == null) {
                            throw new RuntimeException("无法转换类型： String to " + field.getType());
                        }
                        Object realValue = conv.convert(content);
                        field.set(conv,realValue);
                    }
                } catch (NoSuchFieldException e) {
                    continue;
                }
            }
        }  catch (Exception e){
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public Map<Property, Field> getReflection(Class clazz) {
        if (!propertiesMap.containsKey(clazz)) {
            Map<Property,Field> refs = ConfigHandler.super.getReflection(clazz);
            propertiesMap.put(clazz,refs);
        }
        return propertiesMap.get(clazz);
    }

    /**
     * 将map转换为xml格式，
     * 或者从xml读取一个map格式。
     */
    public static class MapEntryConverter implements com.thoughtworks.xstream.converters.Converter {

        public boolean canConvert(Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        /**
         * 将Map序列化为Xml的方法。
         * @param value
         * @param writer
         * @param context
         */
        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {

            AbstractMap map = (AbstractMap) value;
            for (Object obj : map.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;
                writer.startNode(entry.getKey().toString());
                Object val = entry.getValue();
                if ( null != val ) {
                    if (!Reflections.isSystemType(val.getClass())) {
                        // value 是一个Object的时候
                        writer.addAttribute("type",Map.class.getSimpleName());
                        this.writeObjects(val,writer);
                    } else {
                        writer.setValue(val.toString());
                    }
                }
                writer.endNode();
            }

        }

        /**
         * 将Xml反序列化为Map的方法。
         * @param reader
         * @param context
         * @return
         */
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            Map<String, Object> map = new HashMap<>();

            while (reader.hasMoreChildren()) {

                reader.moveDown();

                String key = reader.getNodeName();
                Object value = null;

                String type = reader.getAttribute("type");
                if (type == null) {
                    type = Map.class.getSimpleName();
                }
                value = this.readObjects(type,reader,context);
                map.put(key, value);

                reader.moveUp();
            }

            return map;
        }

        /**
         * 从reader里面读取一个map
         * @param reader
         * @param context
         * @return
         */
        private Object readMap(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Map<String,Object> map = new HashMap<>();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                String type = reader.getAttribute("type");

                if (type == null) {
                    type = Map.class.getSimpleName();
                }

                String key = reader.getNodeName(); // nodeName aka element's name
                Object value = null;

                if (reader.hasMoreChildren()) {
                    value = this.readObjects(type,reader,context);
                } else {
                    value = reader.getValue();
                }
                map.put(key, value);
                reader.moveUp();
            }
            return map;
        }

        private Object readList(HierarchicalStreamReader reader,UnmarshallingContext context) {
            try {
                List<Object> items = new ArrayList<>();
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    String name = reader.getNodeName();
                    Object value = null;
                    if (!name.equals("item")) {
                        throw new RuntimeException("有一个type=list的标签的格式错误，" +
                                "type=list的标签内部只能使用item标签。");
                    }
                    if (reader.hasMoreChildren()) {
                        String type = reader.getAttribute("type");
                        if (type == null)  {
                            type = Map.class.getSimpleName();
                        }
                        value = this.readObjects(type,reader,context);
                    } else {
                        value = reader.getValue();
                    }
                    items.add(value);
                    reader.moveUp();
                }
                return items;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 从reader按照类型读取对象。
         * @param typeName
         * @param reader
         * @param context
         * @return
         */
        private Object readObjects(String typeName, HierarchicalStreamReader reader, UnmarshallingContext context) {
            if (typeName.equals(Map.class.getSimpleName())) {
               return this.readMap(reader,context);
            } else if (typeName.equals(List.class.getSimpleName())) {
               return this.readList(reader,context);
            }
            return null;
        }

        /**
         * 向writer写入map对象
         * @param map
         * @param writer
         */
        private void writeMap(Map<String,Object> map,HierarchicalStreamWriter writer) {
            for (Map.Entry<String,Object> ent: map.entrySet()) {
                Object value = ent.getValue();
                if (value == null) {
                    value = "";
                }
                if (Reflections.isSystemType(value.getClass())) {
                    /// 按字段写入节点。
                    writer.startNode(ent.getKey());
                    writer.setValue(value.toString());
                    writer.endNode();
                } else {
                    writer.startNode(ent.getKey());
                    writer.addAttribute("type",Map.class.getSimpleName());
                    this.writeObjects(value,writer);
                    writer.endNode();
                }
            }
        }

        private void writeList(List<Object> list,HierarchicalStreamWriter writer) {
            for (Object o: list) {
                if (Reflections.isSystemType(o.getClass())) {
                    writer.startNode("item");
                    writer.setValue(o.toString());
                    writer.endNode();
                } else {
                    writeObjects(o,writer);
                }
            }
        }


        /**
         * 向writer写入对象。
         *
         * @param obj
         * @param writer
         */
        private void writeObjects(Object obj, HierarchicalStreamWriter writer) {
            if (obj instanceof Map) {
                // 是map，使用map的写入方法
                Map<String,Object> map = (Map<String, Object>) obj;
                this.writeMap(map,writer);
                return;
            } else if (obj instanceof List) {
                List<Object> list = (List<Object>) obj;
                writeList(list,writer);
                return;
            }
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field: fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value == null) {
                        value = "";
                    }
                    Class type = value.getClass();
                    if (Reflections.isSystemType(type)) {
                        /// 按字段写入节点。
                        writer.startNode(field.getName());
                        writer.setValue(value.toString());
                        writer.endNode();
                    } else  {
                        // 还是一个Object，本方法进行递归写入。
                        writer.startNode(field.getName());
                        if (List.class.isAssignableFrom(field.getType())) {
                            ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                            Class actuals = (Class) paramType.getActualTypeArguments()[0];

                            writer.addAttribute("type",List.class.getSimpleName());
                        } else {
                            writer.addAttribute("type",Map.class.getSimpleName());
                        }
                        writeObjects(value,writer);
                        writer.endNode();
                    }
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
