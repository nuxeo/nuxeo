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
    "description": "Retrieves all OAuth2 tokens.",
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
    "path": "/oauth2/token/search",
    "description": "Search tokens that match service name or user parameter.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"searchOauth2Tokens",
        "type":"oauth2TokenDataList",
        <@params names = ["matchOAuht2Tokens"]/>,
        "summary":"Search tokens that match service name or user parameter.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/token/{oAuth2TokenType}",
    "description": "Retrieves all OAuth2 tokens by OAuth2 token type.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOauth2TokensByType",
        "type":"oauth2TokenDataList",
        <@params names = ["oAuth2TokenType"]/>,
        "summary":"Retrieves all OAuth2 tokens by OAuth2 token type. An OAuth2 token can be provided by Nuxeo or consumed.",
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
        "nickname":"getOauth2UserProviderTokens",
        "type":"oauth2TokenDataList",
        "summary":"Retrieves all OAuth2 provider tokens for the current user.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/token/provider/{oauth2ProviderId}/user/{userName}",
    "description": "Gets, updates and deletes OAuth2 provider tokens.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOauth2ProviderToken",
        "type":"oauth2TokenData",
        <@params names = ["oauth2ProviderId","username"]/>,
        "summary":"Gets an OAuth2 provider token.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"PUT",
        "nickname":"updateOauth2ProviderToken",
        "type":"oauth2TokenData",
        <@params names = ["oauth2ProviderId","username"]/>,
        "summary":"Updates an OAuth2 provider token.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"DELETE",
        "nickname":"deleteOauth2ProviderToken",
        <@params names = ["oauth2ProviderId","username"]/>,
        "summary":"Delete an OAuth2 provider token.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/token/client",
    "description": "Retrieves OAuth2 client tokens for the current user.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOauth2UserClientTokens",
        "type":"oauth2TokenDataList",
        "summary":"Retrieves all OAuth2 client tokens for the current user.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/token/client/{oauth2ClientId}/user/{userName}",
    "description": "Retrieves updates, and deletes OAuth2 client tokens.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOauth2ClientToken",
        "type":"oauth2TokenData",
        <@params names = ["oauth2ClientId", "username"]/>,
        "summary":"Retrieves a OAuth2 client token.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"PUT",
        "nickname":"updateOauth2ClientToken",
        "type":"oauth2TokenData",
        <@params names = ["oauth2ClientId","username"]/>,
        "summary":"Updates an OAuth2 client token.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"DELETE",
        "nickname":"deleteOauth2ClientToken",
        <@params names = ["oauth2ClientId", "username"]/>,
        "summary":"Deletes a OAuth2 client token.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/client",
    "description": "Adds and retrieves all OAuth2 clients.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOauth2Clients",
        "type":"oauth2ClientDataList",
        "summary":"Retrieves a OAuth2 client.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"POST",
        "nickname":"addOAuth2Client",
        "type":"oauth2ClientData",
        <@params names = ["oauth2ClientBody"]/>,
        "summary":"Adds an OAuth2 client.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },

  {
    "path": "/oauth2/client/{oauth2ClientId}",
    "description": "Updates, deletes and retrieves an OAuth2 client.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getOauth2Client",
        "type":"oauth2ClientData",
        <@params names = ["oauth2ClientId"]/>,
        "summary":"Retrieves an OAuth2 client.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"PUT",
        "nickname":"updateOAuth2Client",
        "type":"oauth2ClientData",
        <@params names = ["oauth2ClientBody"]/>,
        "summary":"Update an OAuth2 client.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"DELETE",
        "nickname":"deleteOAuth2Client",
        <@params names = ["oauth2ClientId"]/>,
        "summary":"Deletes an OAuth2 client token.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  }

</@block>

<@block name="models">
  <#include "views/doc/datatypes/oauth2ProviderData.ftl"/>,
  <#include "views/doc/datatypes/oauth2TokenData.ftl"/>,
  <#include "views/doc/datatypes/oauth2ProviderTokenData.ftl"/>,
  <#include "views/doc/datatypes/oauth2ClientData.ftl"/>
</@block>
</@extends>
