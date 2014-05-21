[
<#list operations as op>
{ "id": "${op.id}",
  "label" : "${op.description}",
  "params" : [
  <#list op.getParams() as param>
    {
    "name" : "${param.name}",
    "type" : "${param.type}",
    <#if param.required>
      "required" : true
    <#else>
      "required" : false
    </#if>
    },
  </#list>
  ]
},
</#list>
]