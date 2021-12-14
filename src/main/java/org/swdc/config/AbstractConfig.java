package org.swdc.config;

import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.annotations.Property;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;

public abstract class AbstractConfig {

    protected File sourceFile;
    protected InputStream sourceStream;

    private ConfigHandler handler;

    public AbstractConfig(){
        this.init();
    }

    public void load() {
        try {
            this.handler.load(this);
        } catch (Exception e) {
            System.err.println("配置加载失败，请检查配置文件是否完整，并且和配置类一一对应。");
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            this.handler.save(this);
        }catch (Exception e) {
            throw new RuntimeException("无法存储配置",e);
        }
    }

    private void init(){

        ConfigureSource source = this.getClass().getAnnotation(ConfigureSource.class);

        if (source == null) {

            // 无对应的配置文件，通过initialize方法初始化。
            // 这种类型的config不能存储。
            // 通常用于提供一个临时性的配置。

            if (this instanceof ConfigInitializer) {
                ConfigInitializer initializer = (ConfigInitializer) this;
                initializer.initialize();
            }

            handler = new ConfigHandler() {
                @Override
                public void save(Object configObj) throws IOException {
                }
                @Override
                public void load(Object configObj) throws IOException {
                }
            };

        } else if (source != null) {
            try {

                // 包含配置文件的配置类，需要对配置文件进行读写。
                // 使用提供的handler操作配置文件，进行配置的加载和储存。

                Class handler = source.handler();
                this.handler =(ConfigHandler) handler.getConstructor().newInstance();
                this.load();

            } catch (Exception e) {

            }
        }
    }

    public void setSourceStream(InputStream sourceStream) {
        this.sourceStream = sourceStream;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Map<Property, Field> getConfigureInfo() {
        return this.handler.getConfigureInfo(this.getClass());
    }

}
