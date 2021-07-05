package org.swdc.language;

import com.fasterxml.jackson.databind.JsonNode;

public class DefaultLanguage implements Language {

    private JsonNode lang;

    public DefaultLanguage(JsonNode node) {
        this.lang = node;
    }

    @Override
    public String local(String property) {
        String target = "/" + property.replace(".","/");
        return lang.at(target).asText();
    }

}
