open module swdc.application.configs {

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    requires org.yaml.snakeyaml;
    requires io.github.xstream.mxparser;
    requires xstream;
    requires typesafe.config;

    exports org.swdc.config;
    exports org.swdc.config.annotations;
    exports org.swdc.config.configs;
    exports org.swdc.config.converters;



}