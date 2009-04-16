<#-- Util template to define global macros and variables -->

<#assign user=(Session.getPrincipal())/>
<#assign canWrite=(Session.hasPermission(Document.ref, 'Write'))/>
