<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
  {
    "path": "/path/{docPath}/@acl",
    "description": "View the ACLs of a document",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getACLByPath",
      "responseClass":"Acp",
      <@params names = ["docpath"]/>,
      "summary":"View the ACL of a document given its path",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  },
  {
    "path": "/id/{docId}/@acl",
    "description": "View the ACLs of a document",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getACLByPath",
      "responseClass":"Acp",
      <@params names = ["docid"]/>,
      "summary":"View the ACL of a document given its id",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]

  }
  </@block>

<@block name="models">
  <#include "views/doc/datatypes/acp.ftl"/>
</@block>
</@extends>