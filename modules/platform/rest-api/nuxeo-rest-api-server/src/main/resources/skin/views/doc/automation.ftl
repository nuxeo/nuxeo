<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
  {
    "path": "/automation",
    "description": "Automation base",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getOperationsList",
      "responseClass":"OperationDescriptionList",
      "summary":"Gets the list of all operation/chain",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  },
  {
    "path": "/automation/{operationName}",
    "description": "Execute an operation or a chain on a document",
    "operations" : [
    {
      "method":"POST",
      "nickname":"executeOperation",
      <@params names = ["operationname","operationparams"]/>,
      "consumes":["application/json"],
      "summary":"Execute an operation or a chain on a document",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"GET",
      "nickname":"getOperationDescription",
      "responseClass":"OperationDescription",
      <@params names = ["operationname"]/>,
      "summary":"Gets the description of the operation/chain",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  },

  {
    "path": "/path/{docPath}/@op/{operationName}",
    "description": "Execute an operation or a chain on a document",
    "operations" : [
    {
      "method":"POST",
      "nickname":"executeOperationOnDocByPath",
      <@params names = ["docpath","operationname","operationparams"]/>,
      "consumes":["application/json"],
      "summary":"Execute an operation or a chain on a document",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  },

  {
    "path": "/path/{docPath}/@children/@op/{operationName}",
    "description": "Execute an operation or a chain on the children of a document",
    "operations" : [
    {
      "method":"POST",
      "nickname":"executeOperationOnDocByPath",
      <@params names = ["docpath","operationname","operationparams"]/>,
      "consumes":["application/json"],
      "summary":"Execute an operation or a chain on the children of a document",
      "notes": "This works on every API endpoint that return DocumentList",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  }


</@block>

<@block name="models">
  <#include "views/doc/datatypes/operationparams.ftl"/>
</@block>
</@extends>
