<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
  <@block name="apis">

  {
  "path": "/path/{docPath}/@rendition/{renditionName}",
  "description": "Returns the specified rendition on document by its path",
  "operations" : [
  {
  "method":"GET",
  "nickname":"getRenditionByPath",
  "type":"documents",
    <@params names = ["docpath","renditionName"]/>,
  "summary":"Returns the specified rendition on document by its path",
  "notes": "",
    <#include "views/doc/errorresponses.ftl"/>
  }
  ]

  },


  {
  "path": "/id/{docId}/@rendition/{renditionName}",
  "description": "Returns the specified rendition on document by its id",
  "operations" : [
  {
  "method":"GET",
  "nickname":"getRenditionById",
  "type":"documents",
    <@params names = ["docid","renditionName"]/>,
  "summary":"Returns the specified rendition on document by its id",
  "notes": "",
    <#include "views/doc/errorresponses.ftl"/>
  }
  ]

  }


  </@block>

</@extends>
