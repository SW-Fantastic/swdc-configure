package org.swdc.config;

import java.util.Collection;
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
        return Collection.class.isAssignableFrom(clazz);
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
