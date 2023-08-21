package org.swdc.config;

import org.swdc.ours.common.type.ClassTypeAndMethods;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Reflections {


    public static Boolean isSystemType(Class clazz) {
        if (ClassTypeAndMethods.isBasicType(clazz)) {
            return true;
        }
        if (ClassTypeAndMethods.isBoxedType(clazz)) {
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
