module swdc.application.configs {

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires org.yaml.snakeyaml;
    requires dom4j;

    exports org.swdc.config;
    exports org.swdc.config.annotations;
    exports org.swdc.config.configs;
    exports org.swdc.config.converters;

}