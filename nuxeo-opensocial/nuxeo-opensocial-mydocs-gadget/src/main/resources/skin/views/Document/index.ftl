{ "document": {
     "id":"${Document.id}",
    "document": [
  <#list Document.children as child>
    { "id":"${child.id}", "name":"${child.name}", "title":"${child.title}", "type":"${child.type}", "folderish": "<#if child.type == "Folder" || child.type=="Workspace">1<#else>0</#if>"},
  </#list>
     ]

   }
}