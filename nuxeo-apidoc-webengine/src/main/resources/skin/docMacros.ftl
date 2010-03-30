<#macro docContent docItem>
  <#if docItem.renderingType=='html'>
      ${docItem.content}
  </#if>
  <#if docItem.renderingType=='wiki'>
      <@wiki>${docItem.content}</@wiki>
  </#if>
</#macro>