<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">

  {
    "path": "/oauth2/provider/{oauth2ProviderId}",
    "description": "Retrieves OAuth2 data",
    "operations" : [
      {
        "method":"GET",
        "nickname":"oauth2ProviderData",
        "type":"oauth2ProviderData",
        <@params names = ["oauth2ProviderId"]/>,
        "summary":"Retrieves OAuth2 data for the current user.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/provider/{oauth2ProviderId}/token",
    "description": "Retrieves a valid access token",
    "operations" : [
      {
        "method":"GET",
        "nickname":"oauth2TokenData",
        "type":"oauth2TokenData",
        <@params names = ["oauth2ProviderId"]/>,
        "summary":"Retrieves a valid access token to the current user. A new token will be request if the current one is expired.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  }

</@block>

<@block name="models">
  <#include "views/doc/datatypes/oauth2ProviderData.ftl"/>,
  <#include "views/doc/datatypes/oauth2TokenData.ftl"/>
</@block>
</@extends>
