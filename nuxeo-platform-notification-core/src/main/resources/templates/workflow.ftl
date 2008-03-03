<HTML>
<BODY>
<P>
<#if eventId == "workflowStarted">
Workflow started on document (UID: ${docId}) by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.
<#elseif eventId == "workflowEnded">
Ended workflow for document (UID: ${docId}) by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.
<#elseif eventId == "workflowAbandoned">
Abandoned workflow for document (UID: ${docId}) by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.
<#elseif eventId == "workflowTaskAssigned">
A task was assigned for document (UID: ${docId}) by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.
<#elseif eventId == "workflowTaskUnassigned">
A task was unassigned for document (UID: ${docId}) by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.
<#elseif eventId == "workflowTaskEnded">
Task ended for document (UID: ${docId}) by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.
<#elseif eventId == "workflowTaskRemoved">
Task removed for document (UID: ${docId}) by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.
<#elseif eventId == "workflowTaskSuspended">
A task was suspended for document (UID: ${docId}) by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.
<#elseif eventId == "workflowTaskRejected">
A task was rejected for document (UID: ${docId}) by ${author} at ${dateTime?datetime?string("dd/MM/yyyy - HH:mm")}.
</#if>
<BR>
You can consult the document at the following URL: <a href="${docUrl}">${docTitle}</a></P>
</BODY>
<HTML>