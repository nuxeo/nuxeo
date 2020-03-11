<#macro param name>
  <#include "views/doc/paramtypes/" + name + ".ftl"/>
</#macro>


<#macro params names>
  "parameters": [
  <#list names as name>
    <@param name=name/>
    <#if name_has_next>,</#if>
  </#list>
  ]
</#macro>