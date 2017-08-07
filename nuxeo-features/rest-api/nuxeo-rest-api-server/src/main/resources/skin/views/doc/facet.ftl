<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
  <@block name="apis">
{
  "path": "/config/facets",
  "description": "List registered facets",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getFacets",
      "type":"facet",
      "summary":"List registered facets",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]
},

{
  "path": "/config/facets/{facet}",
  "description": "Facet description",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getFacet",
      "type":"facet",
      "summary":"Facet description",
      <@params names = ["facet"]/>,
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]

}
    </@block>

    <@block name="models">
        <#include "views/doc/datatypes/facet.ftl"/>
    </@block>
</@extends>
