<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
  {
    "path": "/path/{docPath}/@audit",
    "description": "View the audit of a document",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getAuditByPath",
      "type":"LogEntries",
      <@params names = ["docpath"]/>,
      "summary":"View the audit trail of a document given its path",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  },
  {
    "path": "/id/{docId}/@audit",
    "description": "View the audit of a document",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getAuditById",
      "type":"LogEntries",
      <@params names = ["docid"]/>,
      "summary":"View the audit trail of a document given its id",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]

  }


  </@block>

<@block name="models">
  <#include "views/doc/datatypes/logentries.ftl"/>
</@block>
</@extends>