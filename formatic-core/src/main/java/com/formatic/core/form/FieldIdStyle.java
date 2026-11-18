package com.formatic.core.form;

import java.util.Objects;

public enum FieldIdStyle {
    DOT,
    CAMEL_CASE,
    SNAKE_CASE;

    public String format(String path) {
        Objects.requireNonNull(path, "path");
        if (this == DOT) return path;
        if (this == SNAKE_CASE) return path.replace('.', '_');
        StringBuilder id = new StringBuilder();
        for (String segment : path.split("\\.")) {
            if (id.isEmpty()) {
                id.append(segment);
            } else if (!segment.isEmpty()) {
                id.append(Character.toUpperCase(segment.charAt(0))).append(segment.substring(1));
            }
        }
        return id.toString();
    }
}
