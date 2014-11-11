<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">

     {
      "path": "/path/{docPath}/@children",
      "description": "Operation on document's children by its id",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getChildrenByPath",
          "type":"documents",
          <@params names = ["docpath","paging","propheader"]/>,
          "summary":"Get the children of a document by its path",
          "notes": "Only documents which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },


    {
      "path": "/id/{docId}/@children",
      "description": "Operation on document's children by its id",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getChildrenById",
          "type":"documents",
          <@params names = ["docid","paging","propheader"]/>,
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
