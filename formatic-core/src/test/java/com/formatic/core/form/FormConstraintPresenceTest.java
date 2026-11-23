package com.formatic.core.form;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FormConstraintPresenceTest {
    @Test
    void manuallyConstructedFieldsHaveAbsentConstraints() {
        for (var field : new FormFieldMetadata[]{
                new FormFieldMetadata(),
                new FormFieldMetadata("amount", FormFieldType.NUMBER, "Amount", 1)}) {
            assertThat(field.getMin()).isNaN();
            assertThat(field.getMax()).isNaN();
            assertThat(field.getStep()).isNaN();
            assertThat(field.getMinLength()).isEqualTo(-1);
            assertThat(field.getMaxLength()).isEqualTo(-1);
            assertThatCode(() -> FormConstraintValidator.validate(field, "amount")).doesNotThrowAnyException();
        }
    }

    @Test
    void copiesPreserveAbsentAndExplicitZeroConstraints() {
        var original = new FormFieldMetadata("amount", FormFieldType.NUMBER, "Amount", 1);
        var absent = new FormFieldMetadata(original);
        assertThat(absent.getMin()).isNaN();
        assertThat(absent.getMaxLength()).isEqualTo(-1);

        original.setMin(0);
        original.setMax(0);
        original.setStep(1);
        original.setMinLength(0);
        original.setMaxLength(0);
        var explicit = new FormFieldMetadata(original);
        assertThat(explicit.getMin()).isZero();
        assertThat(explicit.getMax()).isZero();
        assertThat(explicit.getStep()).isEqualTo(1);
        assertThat(explicit.getMinLength()).isZero();
        assertThat(explicit.getMaxLength()).isZero();
        assertThatCode(() -> FormConstraintValidator.validate(explicit, "amount")).doesNotThrowAnyException();
    }
}
