
<#--assign tab = Context.getClientVariable("tab") /-->

<#if tab>
  <#include "@${tab}"/>
<#else>
  <#include "@content_page"/>
</#if>
