@import com.formatic.core.form.FormFieldType
@import com.formatic.render.FormRenderModel
@import com.formatic.render.jte.FieldView
@param FormRenderModel form
<form class="row g-3" method="post" action="${form.action()}"
      enctype="${form.fields().stream().anyMatch(field -> field.metadata().getType() == FormFieldType.FILE) ? "multipart/form-data" : null}">
    @if(form.title() != null)
        <h2>${form.title()}</h2>
    @endif
    @if(form.successMessage() != null)
        <div class="alert alert-success" role="status">${form.successMessage()}</div>
    @endif
    @if(!form.globalErrors().isEmpty())
        <ul class="alert alert-danger" role="alert">
            @for(var error : form.globalErrors())
                <li>${error}</li>
            @endfor
        </ul>
    @endif
    @for(var hidden : form.hiddenFields().entrySet())
        <input type="hidden" name="${hidden.getKey()}" value="${hidden.getValue()}">
    @endfor
    @for(var field : form.fields())
        @template.formatic.bootstrap.vertical.field(new FieldView(field))
    @endfor
    <div class="col-12"><button class="btn btn-primary" type="submit">Submit</button></div>
</form>
