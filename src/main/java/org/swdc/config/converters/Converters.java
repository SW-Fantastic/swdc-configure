package org.swdc.config.converters;

import org.swdc.config.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * 转换器类，内置基本类型的相互转换。
 * 通过addConverter可以添加新的类型转换器。
 */
public class Converters {

    private Map<ConvertersKey,Converter> converters = new HashMap<>();

    public Converters() {
        this.addConverter(String.class,int.class, this::covertIntFormString)
                .addConverter(int.class,String.class,this::convertStringFormInt)
                .addConverter(String.class,Integer.class,this::covertIntFormString)
                .addConverter(Integer.class,String.class,this::convertStringFormInt)

                .addConverter(String.class,double.class,this::convertDoubleFormString)
                .addConverter(double.class,String.class,this::convertStringFormDouble)
                .addConverter(String.class,Double.class,this::convertDoubleFormString)
                .addConverter(Double.class,String.class,this::convertStringFormDouble)

                .addConverter(String.class,short.class,this::convertShortFormString)
                .addConverter(short.class,String.class,this::convertStringFormShort)
                .addConverter(String.class,Short.class,this::convertShortFormString)
                .addConverter(Short.class,String.class,this::convertStringFormShort)

                .addConverter(String.class,boolean.class,this::convertBooleanFormString)
                .addConverter(boolean.class,String.class,this::convertStringFormBoolean)
                .addConverter(String.class,Boolean.class,this::convertBooleanFormString)
                .addConverter(Boolean.class,String.class,this::convertStringFormBoolean)

                .addConverter(String.class,float.class,this::convertFloatFormString)
                .addConverter(float.class,String.class,this::convertStringFormFloat)
                .addConverter(String.class,Float.class,this::convertFloatFormString)
                .addConverter(Float.class,String.class,this::convertStringFormFloat)

                .addConverter(Integer.class, int.class, (i) -> i)
                .addConverter(int.class, Integer.class, (i) -> i)
                .addConverter(Float.class, float.class, (i) -> i)
                .addConverter(float.class,Float.class, (i) -> i)
                .addConverter(Double.class,double.class, (d) -> d)
                .addConverter(double.class,Double.class, (d)->d)
                .addConverter(Short.class,short.class,(s) -> s)
                .addConverter(short.class,Short.class, (s) -> s)
                .addConverter(boolean.class,Boolean.class, (b)->b)
                .addConverter(Boolean.class,boolean.class, (b) -> b);

    }

    public <T,R> Converters addConverter(Class<T> t, Class<R> r, Converter<T,R> converter) {
        converters.put(ConvertersKey.of(t,r),converter);
        return this;
    }

    public <T,R> Converter<T,R> getConverter(Class<T> t, Class<R> r) {
        return converters.get(ConvertersKey.of(t,r));
    }

    private int covertIntFormString(String val) {
        return Integer.parseInt(val);
    }

    private double convertDoubleFormString(String val) {
        return Double.parseDouble(val);
    }

    private float convertFloatFormString(String val) {
        return Float.parseFloat(val);
    }

    private short convertShortFormString(String val) {
        return Short.parseShort(val);
    }

    private boolean convertBooleanFormString(String val) {
        return Boolean.parseBoolean(val);
    }

    private String convertStringFormInt(int number) {
        return Integer.toString(number);
    }

    private String convertStringFormFloat(float number) {
        return Float.toString(number);
    }

    private String convertStringFormDouble(double number) {
        return Double.toString(number);
    }

    private String convertStringFormShort(short number) {
        return Short.toString(number);
    }

    private String convertStringFormBoolean(boolean val) {
        return Boolean.toString(val);
    }

}
