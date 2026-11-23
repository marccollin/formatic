package com.formatic.schema.json;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/** Framework-neutral data and presentation trees, fresh and mutable per export. */
public record JsonFormDefinition(ObjectNode schema, ObjectNode ui) {
    public JsonFormDefinition {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(ui, "ui");
    }
}
