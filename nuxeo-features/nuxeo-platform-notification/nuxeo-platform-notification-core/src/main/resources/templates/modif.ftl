<html>
<body>
<p>
<a href="${docUrl}">${htmlEscape(docTitle)}</a>
</p>
<p>
Updated: ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}<br>
Created: ${docCreated?datetime?string("dd/MM/yyyy - HH:mm")}<br>
Author: <a href="${userUrl}">${author}</a>
<#if principalAuthor?? && (principalAuthor.lastName!="" || principalAuthor.firstName!="")>
(${htmlEscape(principalAuthor.firstName)} ${htmlEscape(principalAuthor.lastName)})
</#if>
</p>
<p>
Location: ${docLocation}<br>
<#if document.dublincore.description != "">
Description: ${document.dublincore.description}<br>
</#if>
State: ${docState}<br>
Version: ${docVersion}<br>
<#if docMainFileUrl??>
<a href="${docMainFileUrl}">Download main file from the email</a><br>
</#if>
<a href="${docUrl}?tabIds=%3Aview_comments">Go to "comment" tab of the document</a><br>
<a href="${docUrl}?tabIds=%3ATAB_CONTENT_HISTORY">Go to "history" tab of the document</a><br><br>
<em>
You received this notification because you subscribed to modification on this document or on one of its parents.
</em>
</p>
</body>
</html>