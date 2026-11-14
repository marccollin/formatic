package com.formatic.spring.config;

import com.formatic.core.form.FieldIdStyle;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "formatic")
public class FormaticProperties {
    private final Nested nested = new Nested();

    public Nested getNested() {
        return nested;
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
