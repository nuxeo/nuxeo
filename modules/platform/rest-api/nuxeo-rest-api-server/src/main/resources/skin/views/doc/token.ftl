<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
  {
    "path": "/token",
    "description": "Authentication Token",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getTokens",
      "responseClass":"AuthenticationTokenList",
      <@params names = ["token_app"]/>,
      "summary":"Gets all readable authentication tokens",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"POST",
      "nickname":"createToken",
      "summary":"Acquire new authentication token",
      <@params names = ["token_app", "token_others"]/>,
      "notes": "",
        <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  },
  {
    "path": "/token/{token}",
    "description": "Delete existing token",
    "operations" : [
    {
      "method":"DELETE",
      "nickname":"deleteToken",
      "summary":"Delete an existing authentication token",
      <@params names = ["token"]/>,
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  }

</@block>

<@block name="models">
  <#include "views/doc/datatypes/token.ftl"/>
</@block>
</@extends>
