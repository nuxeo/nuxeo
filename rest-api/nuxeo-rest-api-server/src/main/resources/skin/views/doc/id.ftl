<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">

     {
      "path": "/id/{docId}",
      "description": "Browse documents by their id",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getDocumentById",
          "type":"document",
          <@params names = ["docid","propheader"]/>,
          "summary":"Find a document by its id",
          "notes": "Only documents which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"PUT",
          "nickname":"updateDocumentById",
          "type":"document",
          <@params names = ["docid","docbody","propheader"]/>,
          "summary":"Updates a document by its id",
          "notes": "Only documents which you have permission can be updated. Only the properties part of the document object is taken into account for update",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"DELETE",
          "nickname":"deleteDocumentById",
          <@params names = ["docid","propheader"]/>,
          "summary":"Deletes a document by its id",
          "notes": "Only documents which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"POST",
          "nickname":"createDocumentById",
          "type":"document",
          <@params names = ["repoid","docbody"]/>,
          "summary":"Creates a document by its parent id",
          "notes": "",
          <#include "views/doc/errorresponses.ftl"/>
        }

      ]

    },
    {
      "path": "/repo/{repoId}/id/{docId}",
      "description": "Browse documents by their id",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getDocumentById",
          "type":"document",
          <@params names = ["repoid","docid","propheader"]/>,
          "summary":"Find a document by its id",
          "notes": "Only documents which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"PUT",
          "nickname":"updateDocumentById",
          "type":"document",
          <@params names = ["repoid","docid","docbody","propheader"]/>,
          "summary":"Updates a document by its id",
          "notes": "Only documents which you have permission can be updated. Only the properties part of the document object is taken into account for update",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"DELETE",
          "nickname":"deleteDocumentById",
          <@params names = ["repoid","docid"]/>,
          "summary":"Deletes a document by its id",
          "notes": "Only documents which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        },
        {
          "method":"POST",
          "nickname":"createDocumentById",
          "type":"document",
          <@params names = ["repoid","docid","docbody","propheader"]/>,
          "summary":"Creates a document by its parent id",
          "notes": "",
          <#include "views/doc/errorresponses.ftl"/>
        }

      ]

    }



</@block>

<@block name="models">
  <#include "views/doc/datatypes/document.ftl"/>
</@block>
</@extends>