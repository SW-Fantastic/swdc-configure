package org.swdc.config;

@FunctionalInterface
public interface Converter<T,R> {

    R convert(T t);

}
