<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
  <@block name="apis">

  {
    "path": "/path/{docPath}/@convert",
    "description": "Convert the main Blob of the document",
    "operations" : [
      {
        "method":"GET",
        "nickname":"convertDocumentMainBlobByPath",
        "type":"documents",
        <@params names = ["docpath", "convertName", "convertType", "convertFormat"]/>,
        "summary":"Convert the main Blob of the document",
        "notes": "One of the 'name', 'type' or 'format' parameters must be passed.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/path/{docPath}/@blob/{blobXpath}/@convert",
    "description": "Convert the Blob at the given xpath of the document",
    "operations" : [
      {
        "method":"GET",
        "nickname":"convertDocumentBlobByPath",
        "type":"documents",
        <@params names = ["docpath", "blobxpath", "convertName", "convertType", "convertFormat"]/>,
        "summary":"Convert the Blob at the given xpath of the document",
        "notes": "One of the 'name', 'type' or 'format' parameters must be passed.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/id/{docId}/@convert",
    "description": "Convert the main Blob of the document",
    "operations" : [
      {
        "method":"GET",
        "nickname":"convertDocumentMainBlobById",
        "type":"documents",
        <@params names = ["docid", "convertName", "convertType", "convertFormat"]/>,
        "summary":"Convert the main Blob of the document",
        "notes": "One of the 'name', 'type' or 'format' parameters must be passed.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/id/{docId}/@blob/{blobXpath}/@convert",
    "description": "Convert the Blob at the given xpath of the document",
    "operations" : [
      {
        "method":"GET",
        "nickname":"convertDocumentBlobById",
        "type":"documents",
        <@params names = ["docid", "blobxpath", "convertName", "convertType", "convertFormat"]/>,
        "summary":"Convert the Blob at the given xpath of the document",
        "notes": "One of the 'name', 'type' or 'format' parameters must be passed.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  }

  </@block>

</@extends>
