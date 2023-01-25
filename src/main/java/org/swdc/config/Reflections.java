package org.swdc.config;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Reflections {

    public static boolean isBasicType(Class type) {
        if (type == int.class ||
                type == float.class ||
                type == double.class ||
                type == char.class ||
                type == byte.class ||
                type == short.class) {
            return  true;
        }
        return  false;
    }

    public static boolean isBoxedType(Class type) {
        if (type == Integer.class ||
                type == Float.class ||
                type == Double.class ||
                type == Character.class ||
                type == Byte.class ||
                type == Boolean.class||
                type == Short.class) {
            return  true;
        }
        return  false;
    }

    public static Boolean isCollectionType(Class clazz) {
        return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
    }

    public static Boolean isList(Class clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    public static Boolean isMap(Class clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    public static List<Class> getFieldParameters(Field field) {
        ParameterizedType parameterizedType = (ParameterizedType)field.getGenericType();
        Type[] types = parameterizedType.getActualTypeArguments();
        List<Class> classes = new ArrayList<>();
        if (types == null || types.length == 0) {
            return classes;
        }
        for (Type type : types) {
            classes.add((Class) type);
        }
        return classes;
    }

    public static Boolean isSystemType(Class clazz) {
        if (isBasicType(clazz)) {
            return true;
        }
        if (isBoxedType(clazz)) {
            return true;
        }
        /*if (isCollectionType(clazz)) {
            return true;
        }*/
        if (String.class.equals(clazz)) {
            return true;
        }
        return false;
    }

}
