package com.formatic.spring.config;

import com.formatic.core.form.FieldIdStyle;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "formatic")
public class FormaticProperties {
    private final Nested nested = new Nested();
    private final TemplateFolder jte = new TemplateFolder();
    private final TemplateFolder pebble = new TemplateFolder();
    private final TemplateFolder thymeleaf = new TemplateFolder();

    public Nested getNested() {
        return nested;
    }

    public TemplateFolder getJte() {
        return jte;
    }

    public TemplateFolder getPebble() {
        return pebble;
    }

    public TemplateFolder getThymeleaf() {
        return thymeleaf;
    }

    public static class TemplateFolder {
        /** Relative template folder; empty means the template loader's root. */
        private String templatePrefix = "";

        public String getTemplatePrefix() {
            return templatePrefix;
        }

        public void setTemplatePrefix(String templatePrefix) {
            this.templatePrefix = templatePrefix;
        }
    }

    public static class Nested {
        /** HTML ID format; submitted field names always use dotted Spring binding paths. */
        private FieldIdStyle idStyle = FieldIdStyle.DOT;

        public FieldIdStyle getIdStyle() {
            return idStyle;
        }

        public void setIdStyle(FieldIdStyle idStyle) {
            this.idStyle = java.util.Objects.requireNonNull(idStyle, "idStyle");
        }
    }
}
