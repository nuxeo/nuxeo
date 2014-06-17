<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">

     {
      "path": "/path/{docPath}/@pp/{pageProviderName}",
      "description": "Execute a page provider on document by its path",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getChildrenByPath",
          "type":"documents",
          <@params names = ["docpath","pageprovidername","paging","propheader"]/>,
          "summary":"Execute a page provider on document by its path",
          "notes": "Only documents which you have permission to see will be returned",
          <#include "views/doc/errorresponses.ftl"/>
        }
      ]

    },


    {
      "path": "/id/{docId}/@pp/{pageProviderName}",
      "description": "Execute a page provider on document by its id",
      "operations" : [
        {
          "method":"GET",
          "nickname":"getChildrenById",
          "type":"documents",
          <@params names = ["docid","pageprovidername","paging","propheader"]/>,
          "summary":"Execute a page provider on document by its id",
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
