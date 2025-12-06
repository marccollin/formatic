package com.formatic.core.form;

import java.util.Collections;
import java.util.List;

public class ReflectionFallback implements CachedFormMetadata {
    private final List<FormFieldMetadata> metadata;

    public ReflectionFallback(List<FormFieldMetadata> metadata) {
        this.metadata = Collections.unmodifiableList(metadata);
    }

    @Override
    public List<FormFieldMetadata> getMetadata() {
        return metadata;
    }

}
