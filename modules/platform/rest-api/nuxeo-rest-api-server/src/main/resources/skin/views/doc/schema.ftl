<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
  <@block name="apis">
{
  "path": "/config/schemas",
  "description": "List registered schemas",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getSchemas",
      "type":"schema",
      "summary":"List registered schemas",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]
},

{
  "path": "/config/schemas/{schema}",
  "description": "Schema description",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getSchema",
      "type":"schema",
      "summary":"Schema description",
      <@params names = ["schema"]/>,
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]

}
    </@block>

    <@block name="models">
        <#include "views/doc/datatypes/schema.ftl"/>
    </@block>
</@extends>
