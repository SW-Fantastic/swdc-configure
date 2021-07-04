package org.swdc.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.FileSystem;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigureSource {

    /**
     * 这里是和Assets目录相对的路径。
     * @return
     */
    String value();


    /**
     * 外部还是内部，
     * 内部从module里面读取，只读不能修改。
     * @return
     */
    boolean external() default true;

    /**
     * 应该如何处理这个配置数据
     * @return
     */
    Class handler() default Object.class;

    /**
     * 提供一个class用来定位资源。
     * 使用internal的选项时，请填写任意一个本模块的class，
     * 或者本package的class。
     *
     * @return
     */
    Class loadForm() default Object.class;

    /**
     * 使用FileSystem并指定FileSystem的url。
     * @return
     */
    String filesystem() default "";


}
