<h4>
${Document.dublincore.title}
</h4>
<#assign isRichtext = Document.webpage.isRichtext /> 
<div>
  <#if isRichtext == true>
    ${Document.webpage.content}
  <#else>
    <@nxsiteswiki>${Document.webpage.content}</@nxsiteswiki> 
  </#if>
</div>