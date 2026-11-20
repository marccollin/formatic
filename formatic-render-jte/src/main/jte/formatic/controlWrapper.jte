@import com.formatic.render.internal.FieldView
@import com.formatic.render.internal.FormStyle
@param FieldView view
@param FormStyle style
@param boolean group
@param boolean singleCheckbox
@template.formatic.wrap(css = style.fieldBody(singleCheckbox), content = @`
    @template.formatic.fieldBody(view, style, group, singleCheckbox)
`)
