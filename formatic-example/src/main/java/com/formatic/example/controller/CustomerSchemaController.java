package com.formatic.example.controller;

import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.form.FormDefinition;
import com.formatic.example.dto.CustomerForm;
import com.formatic.schema.json.JsonFormDefinition;
import com.formatic.schema.json.DefaultJsonFormExporter;
import com.formatic.spring.render.SpringFormFieldLabels;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

/** Example route for one application-selected DTO, not a generic class lookup. */
@RestController
public class CustomerSchemaController {
    private final ReflectionFormFieldMetadataBuilder metadata;
    private final MessageSource messages;
    private final DefaultJsonFormExporter exporter = new DefaultJsonFormExporter();

    public CustomerSchemaController(ReflectionFormFieldMetadataBuilder metadata, MessageSource messages) {
        this.metadata = metadata;
        this.messages = messages;
    }

    @GetMapping(path = "/api/forms/customer", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonFormDefinition definition(Locale locale) {
        var fields = SpringFormFieldLabels.resolve(metadata.buildMetadata(CustomerForm.class), messages, locale);
        return exporter.export(new FormDefinition("Customer", fields));
    }
}
