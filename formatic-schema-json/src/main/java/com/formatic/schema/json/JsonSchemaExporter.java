package com.formatic.schema.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formatic.core.form.FormDefinition;

/** Exports a form definition as structured JSON Schema, without request state. */
@FunctionalInterface
public interface JsonSchemaExporter {
    ObjectNode export(FormDefinition form);
}
