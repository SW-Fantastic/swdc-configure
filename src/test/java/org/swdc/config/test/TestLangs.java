package org.swdc.config.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.swdc.language.Language;
import org.swdc.language.Locals;

import java.io.File;

public class TestLangs {


    @Test
    public void testLangLoad() {

        Locals locals = new Locals(new File("Lang/languages.json").toPath());
        Language language = locals.getLanguage("zh_cn");
        Assertions.assertEquals("测试的字符串", language.local("demo"));
        Assertions.assertEquals("测试的字符串A", language.local("test.test1"));
    }


}
