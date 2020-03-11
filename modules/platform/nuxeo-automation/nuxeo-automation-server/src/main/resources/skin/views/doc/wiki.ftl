
{toc:maxLevel=3}

<#list categories?keys as cat>
h1. ${cat}
  <#list categories["${cat}"] as operation>

{multi-excerpt:name=${operation.id}}

h2. ${operation.label}

{html}${operation.description}{html}

h5. General Information

*Category:* ${operation.category}
*Operation Id:* ${operation.id}

h5. Parameters

||Name||Type||Required||Default Value||
<#list operation.params as para>
|<#if para.isRequired()>*</#if>${para.name}<#if para.isRequired()>*</#if>|${para.type}|<#if para.isRequired()>true<#else>false</#if>|${This.getParamDefaultValue(para)} |
</#list>

h5. Signature

*Inputs:* ${This.getInputsAsString(operation)}
*Outputs:* ${This.getOutputsAsString(operation)}

{multi-excerpt}

  </#list>
</#list>
