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
            throw new RuntimeException(e);
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
        try {
            ConfigureSource source = this.getClass().getAnnotation(ConfigureSource.class);
            Class handler = source.handler();
            this.handler =(ConfigHandler) handler.getConstructor().newInstance();

            this.load();

        } catch (Exception e) {

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
