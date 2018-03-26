<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">

  {
    "path": "/oauth2/provider",
    "description": "Adds and retrieves OAuth2 providers",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOAuth2Providers",
        "type":"oauth2ProviderDataList",
        "summary":"Retrieves the list of available OAuth2 providers.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"POST",
        "nickname":"addOAuth2Provider",
        "type":"oauth2ProviderData",
        <@params names = ["oauth2ProviderBody"]/>,
        "summary":"Adds an OAuth2 provider.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/provider/{oauth2ProviderId}",
    "description": "Updates, deletes and retrieves OAuth2 provider data",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOauth2Provider",
        "type":"oauth2ProviderData",
        <@params names = ["oauth2ProviderId"]/>,
        "summary":"Retrieves OAuth2 provider data for the current user.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"PUT",
        "nickname":"updateOAuth2Provider",
        "type":"oauth2ProviderData",
        <@params names = ["oauth2ProviderId","oauth2ProviderBody"]/>,
        "summary":"Updates an OAuth2 provider.",
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
        "nickname":"getOauth2ProviderTokenData",
        "type":"oauth2ProviderTokenData",
        <@params names = ["oauth2ProviderId"]/>,
        "summary":"Retrieves a valid access token to the current user. A new token will be request if the current one is expired.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },
  
  {
    "path": "/oauth2/token",
    "description": "Retrieves OAuth2 tokens.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOauth2Tokens",
        "type":"oauth2TokenDataList",
        "summary":"Retrieves all OAuth2 tokens.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/token/provider",
    "description": "Retrieves OAuth2 provider tokens for the current user.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getProviderUserOauth2Tokens",
        "type":"oauth2TokenDataList",
        "summary":"Retrieves all OAuth2 provider tokens for the current user.",
    <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/token/provider/{oauth2ProviderId}/user/{username}",
    "description": "Gets, updates and deletes OAuth2 tokens.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOauth2Token",
        "type":"oauth2TokenData",
        <@params names = ["oauth2ProviderId","username"]/>,
        "summary":"Gets an OAuth2 token.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"PUT",
        "nickname":"updateOauth2Token",
        "type":"oauth2TokenData",
        <@params names = ["oauth2ProviderId","username"]/>,
        "summary":"Updates an OAuth2 token.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"DELETE",
        "nickname":"deleteOauth2Token",
        <@params names = ["oauth2ProviderId","username"]/>,
        "summary":"Delete an OAuth2 token.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  }

</@block>

<@block name="models">
  <#include "views/doc/datatypes/oauth2ProviderData.ftl"/>,
  <#include "views/doc/datatypes/oauth2TokenData.ftl"/>,
  <#include "views/doc/datatypes/oauth2ProviderTokenData.ftl"/>
</@block>
</@extends>
