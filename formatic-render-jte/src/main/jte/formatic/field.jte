@import com.formatic.core.form.FormFieldType
@import com.formatic.render.internal.FieldView
@import com.formatic.render.internal.FormStyle
@param FieldView view
@param FormStyle style
!{
    var field = view.metadata();
    var type = view.type();
    boolean group = type == FormFieldType.RADIO || (type == FormFieldType.CHECKBOX && field.isMultiple());
    boolean singleCheckbox = type == FormFieldType.CHECKBOX && !group;
}
@if(type == FormFieldType.HIDDEN)
    <input type="hidden" id="${field.getId()}" name="${field.getName()}" value="${view.value()}" disabled="${field.isDisabled()}">
@elseif(type == FormFieldType.LABEL)
    <div class="${style.labelWrapper(field.getOuterCssClass())}" id="${field.getId()}">${field.getLabel()}</div>
@else
    @template.formatic.fieldWrapper(field = field, style = style, group = group, singleCheckbox = singleCheckbox, content = @`
        @template.formatic.controlWrapper(view, style, group, singleCheckbox)
    `)
@endif
