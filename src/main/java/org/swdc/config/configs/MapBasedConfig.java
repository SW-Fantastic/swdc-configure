package org.swdc.config.configs;

import org.swdc.config.AbstractConfig;
import org.swdc.config.Converter;

import java.util.Map;

/**
 * 以<code>Map<String,Object></code>这种形式为基础的配置基类。
 * 封装了通用的方法。
 */
public abstract class MapBasedConfig extends AbstractConfig {

    /**
     * 配置前缀，用于子配置。
     */
    protected String prefix;

    /**
     * 配置的具体数据。
     */
    protected Map<String,Object> properties;

    @Override
    public <T> T getConfig(String key, Class<T> clazz) {
        if (getParent() != null) {
            return getParent().getConfig(prefix + "." + key,clazz);
        }
        String parentKey = key.contains(".") ? key.substring(0,key.lastIndexOf(".")) : key;
        Map<String,Object> properties = getProperties(parentKey);
        Object val = null;
        if (key.contains(".")) {
            // 含有“.”，取Parent的Map，然后从里面获取值。
            key = key.substring(key.lastIndexOf(".") + 1);
            if (properties == null) {
                // parent的map不存在或者不是map
                return null;
            }
        } else if (properties == null) {
            // 不含“.”，直接从根map取值。
            properties = this.properties;
        }
        val = properties.get(key);
        if (val == null) {
            return null;
        }
        if (clazz.isAssignableFrom(val.getClass())) {
            return (T)val;
        }
        Converter converter = getConverters().getConverter(val.getClass(),clazz);
        if (converter != null) {
            return (T)converter.convert(val);
        } else {
            throw new RuntimeException("无法转换类型：" + val.getClass() + " 到 " + clazz + " 请手动添加转换器。");
        }
    }

    @Override
    public void setConfig(String key, Object value) {

        if (getParent() != null) {
            getParent().setConfig(prefix + "." + key,value);
            return;
        }

        String parentKey = key.contains(".") ? key.substring(0,key.lastIndexOf(".")) : key;
        Map<String,Object> properties = getProperties(parentKey);
        Object val = null;
        if (key.contains(".")) {
            key = key.substring(key.lastIndexOf(".") + 1);
            if (properties == null) {
                return;
            }
        } else if (properties == null) {
            properties = this.properties;
        }
        val = properties.get(key);
        if (val == null) {
            return;
        }
        if (val.getClass().isAssignableFrom(value.getClass())) {
            properties.put(key,value);
            return;
        }
        Converter converter = getConverters().getConverter(value.getClass(),val.getClass());
        if (converter != null) {
            val = converter.convert(val);
            properties.put(key,val);
        } else {
            throw new RuntimeException("无法转换类型：" + value.getClass() + " 到 " + val.getClass() + " 请手动添加转换器。");
        }
    }

    protected boolean isKeyExists(String key) {
        Map<String,Object> map = getProperties(key);
        return map != null && !map.isEmpty();
    }

    protected Map<String,Object> getProperties(String key) {
        if (getParent() != null) {
            return ((MapBasedConfig)getParent()).getProperties(prefix + "." + key);
        }
        if (!key.contains(".")) {
            Object val = properties.get(key);
            if (Map.class.isAssignableFrom(val.getClass())) {
                return (Map<String, Object>) val;
            } else {
                return null;
            }
        }
        String current = key;
        Map<String,Object> map = this.properties;
        boolean valid = true;
        while (current.contains(".")) {
            String currKey = current.substring(0,current.indexOf("."));
            if (map.containsKey(currKey) && Map.class.isAssignableFrom(map.get(currKey).getClass())) {
                current = current.substring(current.indexOf(".") + 1);
                map = (Map<String, Object>) map.get(currKey);
            } else {
                valid = false;
            }
        }
        if (!valid || !map.containsKey(current)) {
            return null;
        }
        Object val = map.get(current);
        if (Map.class.isAssignableFrom(val.getClass())) {
            return (Map<String, Object>)val;
        }
        return null;
    }

}
