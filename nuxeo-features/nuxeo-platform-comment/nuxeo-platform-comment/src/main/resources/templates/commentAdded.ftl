<html>
<body>
<p>
${author}
<#if principalAuthor?? && (principalAuthor.lastName!="" || principalAuthor.firstName!="")>
(${htmlEscape(principalAuthor.firstName)} ${htmlEscape(principalAuthor.lastName)})
</#if>
has added a comment on <a href="${docUrl}">${docTitle}</a> at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")} with this content:
</p>
<p style="margin:0px 30px">${htmlEscape(comment_text)}</p>
<br/>
<a href="${docUrl}?tabIds=%3Aview_comments">See all comments</a>
<p>
<em>
You received this notification because you subscribed to 'New comment' notification on this document or on one of its parents.
</em>
</p>
</body>
<html>