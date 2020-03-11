<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
  {
    "path": "/path/{docPath}/@bo/{adapterName}",
    "description": "Manipulate a business object adapter on a document by its path",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getBOByPath",
      "responseClass":"BusinessObject",
      <@params names = ["docpath","adaptername"]/>,
      "summary":"Get the business object adapter on a document given its path",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"PUT",
      "nickname":"updateBOByPath",
      "responseClass":"BusinessObject",
      <@params names = ["docpath","adaptername","businessbody"]/>,
      "summary":"Updates the business object adapter on a document given its path",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

    ]
  },
    {
    "path": "/path/{docPath}/@children/@bo/{adapterName}",
    "description": "Get the list of business object of the children of a document by its path",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getBOByPath",
      "responseClass":"BusinessObjectList",
      <@params names = ["docpath","adaptername"]/>,
      "summary":"Get the list of business object of the children of a document by its path",
      "notes": "<ul><li>If a document can't be adapted, the resulting item will be null</li><li> This type of call works for every API endpoint that responds DocumentList (@search, @pp ...)</li></ul>",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  },

  {
    "path": "/path/{docPath}/@bo/{adapterName}/{docName}",
    "description": "Create a document by its business object",
    "operations" : [
    {
      "method":"POST",
      "nickname":"createBOByPath",
      "responseClass":"BusinessObject",
      <@params names = ["docpath","adaptername","docname"]/>,
      "summary":"Creates a document based on its business object",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>

  }
  ]},





  {
    "path": "/id/{docId}/@bo/{adapterName}",
    "description": "Manipulate a business object adapter on a document by its id",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getBOById",
      "responseClass":"BusinessObject",
      <@params names = ["docid","adaptername"]/>,
      "summary":"Get the business object adapter on a document given its id",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>

  },
  {
      "method":"PUT",
      "nickname":"updateBOById",
      "responseClass":"BusinessObject",
      <@params names = ["docid","adaptername","businessbody"]/>,
      "summary":"Updates the business object adapter on a document given its id",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]},


  {
    "path": "/id/{docId}/@children/@bo/{adapterName}",
    "description": "Get the list of business object of the children of a document by its id",
    "operations" : [
    {
      "method":"GET",
      "nickname":"getBOByPath",
      "responseClass":"BusinessObjectList",
      <@params names = ["docid","adaptername"]/>,
      "summary":"Get the list of business object of the children of a document by its id",
      "notes": "<ul><li>If a document can't be adapted, the resulting item will be null</li><li> This type of call works for every API endpoint that responds DocumentList (@search, @pp ...)</li></ul>",
      <#include "views/doc/errorresponses.ftl"/>
    }
    ]
  },



  {
    "path": "/id/{docId}/@bo/{adapterName}/{docName}",
    "description": "Create a document by its business object",
    "operations" : [
    {
      "method":"POST",
      "nickname":"createBOById",
      "responseClass":"BusinessObject",
      <@params names = ["docid","adaptername","docname"]/>,
      "summary":"Creates a document based on its business object",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>

  }
  ]}

</@block>

<@block name="models">
  <#include "views/doc/datatypes/businessobject.ftl"/>
</@block>
</@extends>
