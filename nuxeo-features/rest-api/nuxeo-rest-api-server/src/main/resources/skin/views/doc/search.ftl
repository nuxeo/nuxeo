<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
  {
    "path": "/search/lang/{queryLanguage}/execute",
    "description": "Performs queries.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"query",
        "type":"documents",
        <@params names = ["queryLanguage", "query","pageSize","currentPageIndex","offset","maxResults","sortBy","sortOrder","queryParams"]/>,
        "summary":"Performs a search query.",
        "notes": "You can have also named parameters in the query. See http://doc.nuxeo.com/x/qAc5AQ",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },
  {
    "path": "/search/lang/{queryLanguage}/bulk/{actionId}",
    "description": "Executes bulk action.",
    "operations" : [
      {
        "method":"POST",
        "nickname":"querybulk",
        "type":"bulkstatus",
    <@params names = ["queryLanguage", "query","queryParams","actionId","bulkParameters"]/>,
        "summary":"Performs a bulk action on the query result.",
        "notes": "You can have also named parameters in the query. See http://doc.nuxeo.com/x/qAc5AQ",
    <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },
  {
    "path": "/search/pp/{providerName}/execute",
    "description": "Executes a page provider.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"pageprovider",
        "type":"documents",
        <@params names = ["providerName","pageSize","currentPageIndex","offset","maxResults","sortBy","sortOrder","queryParams"]/>,
        "summary":"Perform Named Page Provider on the repository",
        "notes": "You can have also named parameters in the query. See http://doc.nuxeo.com/x/qAc5AQ",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },
  {
    "path": "/search/pp/{providerName}/bulk/{actionId}",
    "description": "Executes bulk action.",
    "operations" : [
      {
        "method":"POST",
        "nickname":"pageproviderbulk",
        "type":"bulkstatus",
    <@params names = ["providerName","queryParams","actionId","bulkParameters"]/>,
        "summary":"Performs a bulk action on the query result.",
        "notes": "You can have also named parameters in the query. See http://doc.nuxeo.com/x/qAc5AQ",
    <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },
  {
    "path": "/search/pp/{providerName}",
    "description": "Retrieves a page provider's definition.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getPageProviderDefinition",
        "type":"pageproviderdef",
        <@params names = ["providerName"]/>,
        "summary":"Gets the definition of a page provider.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },
  {
    "path": "/search/saved",
    "description": "Saves and returns saved searches.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getSavedSearches",
        "type":"savedsearches",
        <@params names = ["pageProvider"]/>,
        "summary":"Returns the list of saved searches.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"POST",
        "nickname":"saveSearch",
        "type":"savedsearch",
        <@params names = ["searchbody"]/>,
        "summary":"Saves a search.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },
  {
    "path": "/search/saved/{searchId}",
    "description": "Gets, deletes and updates a saved search.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"getSavedSearch",
        "type":"savedsearch",
        <@params names = ["searchId"]/>,
        "summary":"Return the saved search with the supplied id.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"PUT",
        "nickname":"updateSavedSearch",
        "type":"savedsearch",
        <@params names = ["searchId", "searchbody"]/>,
        "summary":"Updates the saved search with the supplied id.",
        <#include "views/doc/errorresponses.ftl"/>
      },
      {
        "method":"DELETE",
        "nickname":"deleteSavedSearch",
        <@params names = ["searchId"]/>,
        "summary":"Deletes the saved search with the supplied id.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },
  {
    "path": "/search/saved/{searchId}/execute",
    "description": "Executes saved searches.",
    "operations" : [
      {
        "method":"GET",
        "nickname":"executeSavedSearch",
        "type":"documents",
        <@params names = ["searchId","pageSize","currentPageIndex","offset","maxResults","sortBy","sortOrder"]/>,
        "summary":"Executes saved searches, returning its results.",
        <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  },
  {
    "path": "/search/saved/{searchId}/bulk/{actionId}",
    "description": "Executes a bulk action.",
    "operations" : [
      {
        "method":"POST",
        "nickname":"savedsearchbulk",
        "type":"bulkstatus",
    <@params names = ["searchId","actionId","bulkParameters"]/>,
        "summary":"Performs a bulk action on the saved search result.",
    <#include "views/doc/errorresponses.ftl"/>
      }
    ]
  }

</@block>

<@block name="models">
  <#include "views/doc/datatypes/savedsearch.ftl"/>,
  <#include "views/doc/datatypes/document.ftl"/>
</@block>
</@extends>
