<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
  <@block name="apis">
{
  "path": "/config/types",
  "description": "List registered document types",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getDocTypes",
      "type":"docType",
      "summary":"List registered document type",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]
},

{
  "path": "/config/types/{docType}",
  "description": "Document type description",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getDocType",
      "type":"docType",
      "summary":"Document type description",
      <@params names = ["docType"]/>,
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]

}
    </@block>

    <@block name="models">
        <#include "views/doc/datatypes/docType.ftl"/>
    </@block>
</@extends>
