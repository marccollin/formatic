@import gg.jte.Content
@import com.formatic.core.form.FormFieldMetadata
@import com.formatic.render.internal.FormStyle
@param FormFieldMetadata field
@param FormStyle style
@param boolean group
@param boolean singleCheckbox
@param Content content
@if(group)
    <fieldset class="${style.fieldWrapper(field.getOuterCssClass())}" id="${field.getId()}">
        <legend class="${style.labelClass()}">${field.getLabel()}</legend>
        ${content}
    </fieldset>
@else
    <div class="${style.fieldWrapper(field.getOuterCssClass())}">
        @if(!singleCheckbox)
            <label class="${style.labelClass()}" for="${field.getId()}">${field.getLabel()}</label>
        @endif
        ${content}
    </div>
@endif
