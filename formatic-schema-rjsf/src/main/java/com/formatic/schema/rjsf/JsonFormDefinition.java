package com.formatic.schema.rjsf;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/** Data schema and RJSF-specific presentation; JSON trees are request-local and mutable. */
public record JsonFormDefinition(ObjectNode schema, ObjectNode uiSchema) {
    public JsonFormDefinition {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(uiSchema, "uiSchema");
    }
}
