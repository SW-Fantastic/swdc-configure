package org.swdc.config.configs;

import com.typesafe.config.*;
import com.typesafe.config.parser.ConfigDocument;
import com.typesafe.config.parser.ConfigDocumentFactory;
import org.swdc.config.AbstractConfig;
import org.swdc.config.ConfigHandler;
import org.swdc.config.Converter;
import org.swdc.config.Reflections;
import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HOCONConfigHandler <T extends AbstractConfig> implements  ConfigHandler<T> {

    private ConfigureSource source;

    @Override
    public void save(T configObj) throws IOException {
        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }

        OutputStream out = this.getOutputStream(source);
        if (out == null) {
            return;
        }
        try {
            Config config = writeHOCON(null,configObj,ConfigFactory.empty());
            String text = config.root().render(ConfigRenderOptions.defaults()
                    .setComments(true)
                    .setOriginComments(false)
                    .setFormatted(true)
                    .setJson(false)
            );
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void load(T configObj) throws IOException {

        if (this.source == null) {
            source = configObj.getClass().getAnnotation(ConfigureSource.class);
        }
        try {
            InputStream in = this.getInputStream(source);
            if (source.external()) {
                File configFile = new File(source.value());
                configObj.setSourceFile(configFile);
            }

            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            Config theConfig = ConfigFactory.parseReader(reader);
            readHOCON(configObj,theConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void readHOCON(Object configObj,Config theConfig) {
        Map<Property, Field> fields =  getReflection(configObj.getClass());
        for (Map.Entry<Property,Field> ent : fields.entrySet()) {
            Property prop = ent.getKey();
            Field target = ent.getValue();
            try {
                target.setAccessible(true);
                if (Reflections.isSystemType(target.getType())) {
                    String value = theConfig.getString(prop.value());
                    if (target.getType() == String.class) {
                        target.set(configObj,value);
                    } else {
                        Converter converter = converters.getConverter(String.class,target.getType());
                        if (converter == null) {
                            throw new RuntimeException("类型转换失败，无法从String转换为：" + target.getType().getName());
                        }
                        target.set(configObj,converter.convert(value));
                    }
                } else if (Reflections.isCollectionType(target.getType())) {
                    if (Reflections.isList(target.getType())) {
                        List<Class> typeParam = Reflections.getFieldParameters(target);
                        if (typeParam.size() == 0) {
                            continue;
                        }
                        Class listType = typeParam.get(0);
                        if (listType == String.class) {
                            target.set(configObj,theConfig.getStringList(prop.value()));
                        } else {
                            List<String> val = theConfig.getStringList(prop.value());
                            Converter converter = converters.getConverter(String.class,listType);
                            if (converter == null) {
                                throw new RuntimeException("can not converter type " + listType.getName() + " from string.");
                            }
                            List targetVal = new ArrayList();
                            for (String item: val) {
                                targetVal.add(converter.convert(item));
                            }
                            target.set(configObj,targetVal);
                        }
                    } else if (Reflections.isMap(target.getType())) {
                        List<Class> paramTypes = Reflections.getFieldParameters(target);
                        if (paramTypes.size() < 2) {
                            continue;
                        }
                        Class keyType = paramTypes.get(0);
                        Class valType = paramTypes.get(1);

                        Map resolved = new HashMap();
                        Config subConf = theConfig.getConfig(prop.value());
                        for (Map.Entry<String,ConfigValue> value : subConf.entrySet()) {
                            Object key = null;
                            Object val = null;
                            if (keyType == String.class) {
                                key = value.getKey();
                            } else {
                                Converter converter = converters.getConverter(String.class, keyType);
                                if (converter == null) {
                                    throw new RuntimeException("can not converter string to " + keyType.getName());
                                }
                                key = converter.convert(value.getKey());
                            }

                            if (valType == String.class) {
                                val = subConf.getString(value.getKey());
                            } else {
                                String theval = subConf.getString(value.getKey());
                                Converter converter = converters.getConverter(String.class,valType);
                                if (converter == null) {
                                    throw new RuntimeException("can not converter string to " + keyType.getName());
                                }
                                val = converter.convert(theval);
                            }

                            resolved.put(key,val);
                        }
                        target.set(configObj,resolved);
                    }
                } else {
                    Object targetObj = target.getType().getConstructor().newInstance();
                    readHOCON(targetObj,theConfig.getConfig(prop.value()));
                    target.set(configObj,targetObj);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    private Config writeHOCON(String prefix, Object configObj,Config theConfig) {
        Map<Property, Field> fields =  getReflection(configObj.getClass());
        for (Map.Entry<Property,Field> ent : fields.entrySet()) {
            String prop = ent.getKey().value();
            Field target = ent.getValue();
            if (prefix != null) {
                prop = prefix + "." + prop;
            }
            try {
                target.setAccessible(true);
                if (Reflections.isSystemType(target.getType())) {
                    Object value = target.get(configObj);
                    if (value.getClass() == String.class) {
                        theConfig = theConfig.withValue(prop,ConfigValueFactory.fromAnyRef(value));
                    } else {
                        Converter converter = converters.getConverter(target.getType(),String.class);
                        if (converter == null) {
                            throw new RuntimeException("类型转换失败，无法把String转换为：" + target.getType().getName());
                        }
                        theConfig = theConfig.withValue(prop,ConfigValueFactory.fromAnyRef(converter.convert(value)));
                    }
                } else if (Reflections.isCollectionType(target.getType())) {
                    if (Reflections.isList(target.getType())) {
                        List<Class> typeParam = Reflections.getFieldParameters(target);
                        if (typeParam.size() == 0) {
                            continue;
                        }
                        Class listType = typeParam.get(0);
                        if (listType == String.class) {
                            theConfig = theConfig.withValue(
                                    prop,
                                    ConfigValueFactory.fromIterable((List)target.get(configObj))
                            );
                        } else {
                            List val = (List) target.get(configObj);
                            Converter converter = converters.getConverter(listType,String.class);
                            if (converter == null) {
                                throw new RuntimeException("can not converter type " + listType.getName() + " to string.");
                            }
                            List targetVal = new ArrayList();
                            for (Object item: val) {
                                targetVal.add(converter.convert(item));
                            }
                            theConfig = theConfig.withValue(prop,ConfigValueFactory.fromIterable(targetVal));
                        }
                    } else if (Reflections.isMap(target.getType())) {
                        List<Class> paramTypes = Reflections.getFieldParameters(target);
                        if (paramTypes.size() < 2) {
                            continue;
                        }
                        Class keyType = paramTypes.get(0);
                        Class valType = paramTypes.get(1);

                        Map<Object,Object> resolved = (Map) target.get(configObj);
                        Map<String,String> config = new HashMap<>();
                        for (Map.Entry<Object,Object> value : resolved.entrySet()) {
                            Object key = null;
                            Object val = null;
                            if (keyType == String.class) {
                                key = value.getKey();
                            } else {
                                Converter converter = converters.getConverter(keyType,String.class);
                                if (converter == null) {
                                    throw new RuntimeException("can not converter string to " + keyType.getName());
                                }
                                key = converter.convert(value.getKey());
                            }

                            if (valType == String.class) {
                                val = value.getValue();
                            } else {
                                Object theval = value.getValue();
                                Converter converter = converters.getConverter(valType,String.class);
                                if (converter == null) {
                                    throw new RuntimeException("can not converter string to " + keyType.getName());
                                }
                                val = converter.convert(theval);
                            }

                            config.put(key.toString(),val.toString());
                        }
                        theConfig = theConfig.withValue(prop,ConfigValueFactory.fromMap(config));
                    }
                } else {
                    Object targetObj = target.get(configObj);
                    theConfig = writeHOCON(prop,targetObj,theConfig);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        return theConfig;
    }

}
