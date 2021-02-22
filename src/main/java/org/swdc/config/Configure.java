package org.swdc.config;


public interface Configure {

    Configure getConfig(String key);

    <T> T getConfig(String key, Class<T> clazz);

    void setConfig(String key, Object value);

    void save();

}
