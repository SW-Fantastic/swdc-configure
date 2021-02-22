package org.swdc.config;

import org.swdc.config.annotations.Property;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置类请继承此类。
 *
 * 通过Property标记的字段，本类可以将配置映射到
 * 注解标注的字段上面。
 *
 * 字段的类型可以是任何能够从配置通过Converter转换得到的类型，
 * 也可以是其他继承了本类的子配置。
 */
public abstract class AbstractConfigure {

    private AbstractConfigure declareOn;
    private List<Field> fields;
    private Configure configure;

    /**
     * 初始化配置
     * @param configure 配置对象
     */
    public AbstractConfigure(Configure configure) {
        this.configure = configure;
        this.fields = getFields(this.getClass());
        for (Field field: fields) {
            Property property = field.getAnnotation(Property.class);
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                try {
                    field.setAccessible(true);
                    Configure subConfig = configure.getConfig(property.value());
                    if (subConfig != null) {
                        if (AbstractConfigure.class.isAssignableFrom(field.getType())) {
                            AbstractConfigure subConfObj = (AbstractConfigure) field.getType()
                                    .getConstructor(Configure.class)
                                    .newInstance(subConfig);
                            subConfObj.setDeclareOn(this);
                            field.set(this,subConfObj);
                            return null;
                        }
                        if (Configure.class.isAssignableFrom(field.getType())){
                            field.set(this,subConfig);
                            return null;
                        }
                        throw new RuntimeException("复合配置字段必须继承ModifiableConfigure或者是Configure。");
                    }  else {
                        field.set(this,configure.getConfig(property.value(),field.getType()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("无法设置属性：" + property.value(),e);
                }
                return null;
            });
        }
    }

    /**
     * 本配置的父配置。
     * @param declareOn
     */
    private void setDeclareOn(AbstractConfigure declareOn) {
        this.declareOn = declareOn;
    }

    /**
     * 更新字段的值到Configure。
     */
    private void sync() {
        for (Field field: fields) {
            try {
                Property property = field.getAnnotation(Property.class);
                if (field.getType().isAssignableFrom(AbstractConfigure.class)) {
                    AbstractConfigure configure = (AbstractConfigure)field.get(this);
                    configure.sync();
                } else if (Configure.class.isAssignableFrom(field.getType())) {
                    continue;
                } else {
                    String key = property.value();
                    configure.setConfig(key,field.get(this));
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * 保存配置文件
     */
    public void save() {
        this.sync();
        if (declareOn != null) {
            declareOn.save();
        } else {
            this.configure.save();
        }
    }

    /**
     * 获取类的配置字段
     * @param clazz 类
     * @return 字段列表
     */
    private List<Field> getFields(Class clazz) {
        List<Field> fieldList = new ArrayList<>();
        Class current = clazz;
        while (current != null) {
            Field[] fields = current.getDeclaredFields();
            for (Field field: fields) {
                if (field.getAnnotation(Property.class) != null) {
                    fieldList.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fieldList;
    }

}
