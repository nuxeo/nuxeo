<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
  {
    "path": "/drive/configuration",
    "description": "Get the Nuxeo Drive configuration",
    "operations" : [ {
      "method":"GET",
      "nickname":"getNuxeoDriveConfiguration",
      "type":"blob",
      "summary":"Get the Nuxeo Drive configuration",
      <#include "views/doc/errorresponses.ftl"/>
    }]
  }
</@block>
</@extends>
