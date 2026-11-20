@import com.formatic.render.FormRenderModel
@import com.formatic.render.internal.FieldView
@import com.formatic.render.internal.FormView
@import com.formatic.render.internal.FormStyle
@param FormRenderModel form
@param FormStyle style
<form class="${style.formClass()}" method="post" action="${form.action()}"
      enctype="${FormView.multipart(form) ? "multipart/form-data" : null}">
    @if(form.title() != null)
        <h2 class="${style.titleClass()}">${form.title()}</h2>
    @endif
    @if(form.successMessage() != null)
        <div class="${style.successClass()}" role="status">${form.successMessage()}</div>
    @endif
    @if(!form.globalErrors().isEmpty())
        <ul class="${style.globalErrorClass()}" role="alert">
            @for(var error : form.globalErrors())
                <li>${error}</li>
            @endfor
        </ul>
    @endif
    @for(var hidden : form.hiddenFields().entrySet())
        <input type="hidden" name="${hidden.getKey()}" value="${hidden.getValue()}">
    @endfor
    @template.formatic.wrap(css = style.fieldsGrid(), content = @`
        @for(var field : form.fields())
            @template.formatic.field(new FieldView(field), style)
        @endfor
        <div class="${style.submitWrapper()}"><button class="${style.submitClass()}" type="submit">Submit</button></div>
    `)
</form>
