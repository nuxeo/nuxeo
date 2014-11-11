<HTML>
<BODY>
<P>
<#if eventId == "documentPublished">
A new document (UID: ${docId}) has been published by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.<BR>
You can consult the document at the following URL: <a href="${docUrl}">${htmlEscape(docTitle)}</a>.
<#elseif eventId == "documentPublicationApproved">
A document (UID: ${docId}) has been approved by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.<BR>
Comment: ${comment}.<BR>
You can consult the document at the following URL: <a href="${docUrl}">${htmlEscape(docTitle)}</a>.
<#elseif eventId == "documentPublicationRejected">
A document (UID: ${docId}) has been rejected by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.<BR>
Comment: ${comment}.<BR>
</#if>
</P>
</BODY>
<HTML>