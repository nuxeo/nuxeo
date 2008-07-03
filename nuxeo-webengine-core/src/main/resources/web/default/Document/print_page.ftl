
<#assign tab = Context.getClientContext("tab").getValue() />

<#if tab>
  <#include "@@${tab}"/>
<#else>
  <#include "@@content_page"/>
</#if>
