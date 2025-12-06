package com.formatic.core.form;

import java.util.Collections;
import java.util.List;

public class BytecodeAccessor implements CachedFormMetadata {
    private final List<FormFieldMetadata> metadata;
    private final Object generatedAccessor;

    public BytecodeAccessor(List<FormFieldMetadata> metadata, Object generatedAccessor) {
        this.metadata = Collections.unmodifiableList(metadata);
        this.generatedAccessor = generatedAccessor;
    }

    @Override
    public List<FormFieldMetadata> getMetadata() {
        return metadata;
    }

}
