@import gg.jte.Content
@param String css
@param Content content
@if(css == null)
    ${content}
@else
    <div class="${css}">${content}</div>
@endif
