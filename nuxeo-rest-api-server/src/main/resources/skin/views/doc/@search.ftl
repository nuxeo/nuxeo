<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">


     {
      "path": "/path/{docPath}/@search",
      "description": "Operation on document's children by its id",
      "operations" : [
        {
          "method":"GET",
          "nickname":"searchFromPath",
          "responseClass":"DocumentList",
          "type":"Document",
          <@params names = ["docpath","paging","search","propheader"]/>,
          "summary":"Get the children of a document by its path",
          "notes": "Only documents which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },


    {
      "path": "/path/{docId}/@search",
      "description": "Operation on document's children by its id",
      "operations" : [
        {
          "method":"GET",
          "nickname":"searchFromId",
          "responseClass":"DocumentList",
          "type":"Document",
          <@params names = ["docid","search","paging","propheader"]/>,
          "summary":"Get the children of a document by its id",
          "notes": "Only documents which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    }


</@block>

<@block name="models">
  <#include "views/doc/datatypes/document.ftl"/>
</@block>
</@extends>