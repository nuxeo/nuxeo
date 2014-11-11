<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
 {
  "path": "/query/{providerName}",
  "description": "Perform queries or page providers.",
  "operations" : [
    {
      "method":"GET",
      "nickname":"pageprovider",
      "type":"search",
      <@params names = ["providerName","pageSize","currentPageIndex","maxResults","sortBy","sortOrder","queryParams"]/>,
      "summary":"Perform Named Page Provider on the repository",
      "notes": "You can have also named parameters in the query. See http://doc.nuxeo.com/x/qAc5AQ",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]
 },
 {
  "path": "/query",
  "description": "Perform queries or page providers.",
  "operations" : [
    {
      "method":"GET",
      "nickname":"query",
      "type":"search",
      <@params names = ["query","pageSize","currentPageIndex","maxResults","sortBy","sortOrder","queryParams"]/>,
      "summary":"Perform Named Page Provider on the repository",
      "notes": "You can have also named parameters in the query. See http://doc.nuxeo.com/x/qAc5AQ",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]
 }


</@block>

<@block name="models">
  <#include "views/doc/datatypes/query.ftl"/>
</@block>
</@extends>