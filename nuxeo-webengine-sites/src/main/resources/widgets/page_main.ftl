<#assign isRichtext = Document.webpage.isRichtext /> 
<div>
  <#if isRichtext == true>
    ${Document.webpage.content}
  <#else>
    <@wiki>${Document.webpage.content}</@wiki> 
  </#if>
</div>