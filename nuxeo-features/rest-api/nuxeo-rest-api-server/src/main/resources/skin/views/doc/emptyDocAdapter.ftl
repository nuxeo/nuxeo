<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
  {
    "path": "/@emptyWithDefault",
    "description": "Initialize an empty document with default properties given a document type",
    "operations" : [{
      "method":"GET",
      "nickname":"getEmptyDocument",
      "type":"document",
      <@params names = ["emptyDocType","emptyDocName"]/>,
      "summary":"Initialize an empty document with default properties given a document type",
      "notes": "To retrieve all initialized properties, the 'properties: *' header must be used",
      <#include "views/doc/errorresponses.ftl"/>
    }]
  },
  {
    "path": "/path/{docPath}/@emptyWithDefault",
    "description": "Initialize an empty document with default properties given a document type and a parent",
    "operations" : [{
      "method":"GET",
      "nickname":"getEmptyDocumentByPath",
      "type":"document",
      <@params names = ["docpath","emptyDocType","emptyDocName"]/>,
      "summary":"Initialize an empty document with default properties given a document type and a parent",
      "notes": "To retrieve all initialized properties, the 'properties: *' header must be used",
      <#include "views/doc/errorresponses.ftl"/>
    }]
  },
  {
    "path": "/id/{docId}/@emptyWithDefault",
    "description": "Initialize an empty document with default properties given a document type and a parent",
    "operations" : [{
      "method":"GET",
      "nickname":"getEmptyDocumentById",
      "type":"document",
      <@params names = ["docid","emptyDocType","emptyDocName"]/>,
      "summary":"Initialize an empty document with default properties given a document type and a parent",
      "notes": "To retrieve all initialized properties, the 'properties: *' header must be used",
      <#include "views/doc/errorresponses.ftl"/>

    }]
  }

</@block>

</@extends>
