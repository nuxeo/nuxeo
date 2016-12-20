Test doc: ${doc.dublincore.title}

<#list doc?keys as k>
#${k}#
</#list>

<#list doc?values as val>
  <#if val??>
    <#if val?is_enumerable>
Collection
    <#else>
Scalar
    </#if>
  <#else>
null
  </#if>
</#list>

${doc.dublincore.title}
${doc.dublincore.issued?datetime}
<#list doc.dublincore.subjects as subject>${subject}|</#list>

<#list doc.files.files as file>
${file.file.filename} ${file.file.length}
</#list>
