@import com.formatic.core.form.FormFieldType
@import com.formatic.render.internal.FieldView
@param FieldView view
!{var field = view.metadata(); var type = view.type();}
@if(type == FormFieldType.HIDDEN)
    <input type="hidden" id="${field.getId()}" name="${field.getName()}" value="${view.value()}" disabled="${field.isDisabled()}">
@elseif(type == FormFieldType.LABEL)
    <div class="${FieldView.nonEmpty(field.getOuterCssClass())}" id="${field.getId()}">${field.getLabel()}</div>
@else
    <div class="${FieldView.nonEmpty(field.getOuterCssClass())}">
        @if(view.needsMarker())
            <input type="hidden" name="${"_" + field.getName()}" value="on">
        @endif
        @if(type == FormFieldType.RADIO || (type == FormFieldType.CHECKBOX && field.isMultiple()))
            <fieldset id="${field.getId()}">
                <legend class="form-label">${field.getLabel()}</legend>
                @for(var option : view.options())
                    !{String id = field.getId() + "_" + option.getValue();}
                    <div class="form-check">
                        <input type="${type == FormFieldType.RADIO ? "radio" : "checkbox"}"
                               class="${view.controlClass("form-check-input")}" id="${id}" name="${field.getName()}"
                               value="${option.getValue()}" checked="${view.selected(option)}"
                               disabled="${field.isDisabled() || option.isDisabled()}"
                               required="${type == FormFieldType.RADIO && field.isRequired()}"
                               aria-invalid="${view.invalid() ? "true" : null}" aria-describedby="${view.describedBy()}">
                        <label class="form-check-label" for="${id}">${option.getLabel()}</label>
                    </div>
                @endfor
            </fieldset>
        @elseif(type == FormFieldType.CHECKBOX)
            <div class="form-check">
                <input type="checkbox" class="${view.controlClass("form-check-input")}" id="${field.getId()}"
                       name="${field.getName()}" value="true" checked="${view.checked()}"
                       required="${field.isRequired()}" disabled="${field.isDisabled()}"
                       aria-invalid="${view.invalid() ? "true" : null}" aria-describedby="${view.describedBy()}">
                <label class="form-check-label" for="${field.getId()}">${field.getLabel()}</label>
            </div>
        @else
            <label class="form-label" for="${field.getId()}">${field.getLabel()}</label>
            @if(type == FormFieldType.SELECT)
                <select class="${view.controlClass("form-select")}" id="${field.getId()}" name="${field.getName()}"
                        multiple="${field.isMultiple()}" required="${field.isRequired()}" disabled="${field.isDisabled()}"
                        aria-invalid="${view.invalid() ? "true" : null}" aria-describedby="${view.describedBy()}">
                    @if(!field.isRequired() && !field.isMultiple())
                        <option value="">Select…</option>
                    @endif
                    @for(var option : view.options())
                        <option value="${option.getValue()}" selected="${view.selected(option)}"
                                disabled="${option.isDisabled()}">${option.getLabel()}</option>
                    @endfor
                </select>
            @elseif(type == FormFieldType.TEXTAREA)
                <textarea class="${view.controlClass("form-control")}" id="${field.getId()}" name="${field.getName()}"
                          required="${field.isRequired()}" disabled="${field.isDisabled()}" readonly="${field.isReadonly()}"
                          rows="${field.getRows() > 0 ? Integer.toString(field.getRows()) : null}"
                          cols="${field.getCols() > 0 ? Integer.toString(field.getCols()) : null}"
                          minlength="${field.getMinLength() > 0 ? Integer.toString(field.getMinLength()) : null}"
                          maxlength="${field.getMaxLength() >= 0 ? Integer.toString(field.getMaxLength()) : null}"
                          wrap="${FieldView.nonEmpty(field.getWrap())}" placeholder="${FieldView.nonEmpty(field.getPlaceholder())}"
                          aria-invalid="${view.invalid() ? "true" : null}" aria-describedby="${view.describedBy()}">${view.value()}</textarea>
            @else
                <input type="${view.inputType()}" id="${field.getId()}" name="${field.getName()}" value="${view.value()}"
                       class="${view.inputClass()}"
                       required="${field.isRequired()}" disabled="${field.isDisabled()}" readonly="${field.isReadonly()}"
                       minlength="${view.supportsLength() && field.getMinLength() > 0 ? Integer.toString(field.getMinLength()) : null}"
                       maxlength="${view.supportsLength() && field.getMaxLength() >= 0 ? Integer.toString(field.getMaxLength()) : null}"
                       min="${view.min()}" max="${view.max()}" step="${view.step()}"
                       alpha="${type == FormFieldType.COLOR && field.isAlpha() ? "alpha" : null}"
                       accept="${type == FormFieldType.FILE ? FieldView.nonEmpty(field.getAccept()) : null}"
                       multiple="${(type == FormFieldType.FILE || type == FormFieldType.EMAIL) && field.isMultiple()}"
                       pattern="${FieldView.nonEmpty(field.getPattern())}" title="${FieldView.nonEmpty(field.getTitle())}"
                       placeholder="${FieldView.nonEmpty(field.getPlaceholder())}"
                       aria-invalid="${view.invalid() ? "true" : null}" aria-describedby="${view.describedBy()}">
            @endif
        @endif
        @if(view.hasHelp())
            <small class="form-text" id="${field.getId() + "-help"}">${field.getHelpText()}</small>
        @endif
        @if(view.invalid())
            <div class="invalid-feedback d-block" id="${field.getId() + "-errors"}" role="alert">
                @for(var error : view.field().errors())
                    <div>${error}</div>
                @endfor
            </div>
        @endif
    </div>
@endif
