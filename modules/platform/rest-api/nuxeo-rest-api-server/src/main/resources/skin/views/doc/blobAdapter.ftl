<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
  <@block name="apis">

  {
  "path": "/path/{docPath}/@blob/{fieldPath}",
  "description": "Get main document blob",
  "operations" : [
  {
  "method":"GET",
  "nickname":"getBlob",
  <@params names = ["docpath", "fieldpath"]/>,
  "summary":"Get the main blob of a document by its path",
    <#include "views/doc/errorresponses.ftl"/>
  }
  ]

  },


  {
  "path": "/id/{docId}/@blob/{fieldPath}",
  "description": "Get main document blob",
  "operations" : [
  {
  "method":"GET",
  "nickname":"getBlob",
  <@params names = ["docid", "fieldpath"]/>,
  "summary":"Get the main blob of a document by its id",
    <#include "views/doc/errorresponses.ftl"/>
  }
  ]

  }

  </@block>
</@extends>
