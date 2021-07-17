package org.swdc.config.configs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.swdc.config.AbstractConfig;
import org.swdc.config.ConfigHandler;
import org.swdc.config.Converter;
import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Json类型的配置文件的处理。
 * @param <T>
 */
public class JsonConfigHandler <T extends AbstractConfig> implements ConfigHandler<T> {

    private ObjectMapper mapper = new ObjectMapper();
    private JsonNode root;
    private ConfigureSource source;

    @Override
    public void save(AbstractConfig configObj) throws IOException {
        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }

        OutputStream out = this.getOutputStream(source);
        if (out == null) {
            return;
        }
        try {
            Map<Property,Field> properties = this.getReflection(configObj.getClass());

            Map<String,Object> result = new HashMap<>();

            for (Map.Entry<Property,Field> ent: properties.entrySet()) {
                Property prop = ent.getKey();
                Field target = ent.getValue();
                String[] segs = prop.value().split("[.]");
                if (segs.length > 0) {
                    Map current = result;
                    for (int idx = 0; idx < segs.length - 1; idx++) {
                        String seg = segs[idx];
                        if (!current.containsKey(seg)) {
                            Map<String,Object> map = new HashMap<>();
                            current.put(seg,map);
                            current = map;
                        } else {
                            Object next = current.get(seg);
                            if (!Map.class.isAssignableFrom(next.getClass())) {
                                throw new RuntimeException("遇到了未知的错误。");
                            }
                            current = (Map) next;
                        }
                    }
                    current.put(segs[segs.length - 1],target.get(configObj));
                }
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(out,result);
        } catch (Exception e) {
            throw new RuntimeException("无法存储配置",e);
        }
    }

    @Override
    public void load(AbstractConfig configObj) {
        try {

            if (this.source == null) {
                source = configObj.getClass().getAnnotation(ConfigureSource.class);
            }
            InputStream in = this.getInputStream(source);
            if (source.external()) {
                File configFile = new File(source.value());
                configObj.setSourceFile(configFile);
            }

            root = mapper.readTree(in);
            Map<Property,Field> properties = this.getReflection(configObj.getClass());
            for (Map.Entry<Property,Field> ent: properties.entrySet()) {
                Property prop = ent.getKey();
                Field target = ent.getValue();

                String path = "/" + prop.value().replace(".","/");
                JsonNode valueNode = root.at(path);
                if (valueNode == null || valueNode.isNull()) {
                    continue;
                }
                if (valueNode.isValueNode()) {
                    String value = valueNode.asText();
                    if (target.getType() == String.class) {
                        target.set(configObj,value);
                        continue;
                    }
                    Converter conv = converters.getConverter(String.class,target.getType());
                    if (conv == null) {
                        throw new RuntimeException("无法转换类型： String to " + target.getType());
                    }
                    Object realValue = conv.convert(value);
                    target.set(configObj,realValue);
                } else {
                    String value = valueNode.toPrettyString();
                    Object realValue = mapper.readValue(value,target.getType());
                    target.set(configObj,realValue);
                }
            }
        }  catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
