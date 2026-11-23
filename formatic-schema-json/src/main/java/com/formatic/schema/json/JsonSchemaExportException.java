package com.formatic.schema.json;

/** Invalid metadata or a feature that cannot be exported without losing semantics. */
public final class JsonSchemaExportException extends IllegalArgumentException {
    public JsonSchemaExportException(String path, String feature) {
        super(path + ": " + feature);
    }

    public JsonSchemaExportException(String path, String feature, IllegalArgumentException cause) {
        super(path + ": " + feature, cause);
    }
}
