<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
 {
  "path": "/group/{groupName}",
  "description": "Operation on groups",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getGroupByName",
      "responseClass":"NuxeoGroup",
      "type":"Document",
      <@params names = ["groupname"]/>,
      "summary":"Get a group by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"PUT",
      "nickname":"updateGroupByName",
      "responseClass":"NuxeoGroup",
      "type":"Document",
      <@params names = ["groupname","groupbody"]/>,
      "summary":"Update a group by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"DELETE",
      "nickname":"deleteGroupByName",
      "type":"Document",
      <@params names = ["groupname"]/>,
      "summary":"Delete a group by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]
},{
  "path": "/group/search",
  "description": "Operation on groups",
  "operations" : [
    {
      "method":"GET",
      "nickname":"searchGroup",
      "responseClass":"NuxeoGroupList",
      "type":"Document",
      <@params names = ["groupquery"]/>,
      "summary":"Search a group",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]

},



{
  "path": "/group",
  "description": "Operation on groups",
  "operations" : [
    {
      "method":"POST",
      "nickname":"createGroup",
      "responseClass":"NuxeoGroup",
      "type":"Document",
      <@params names = ["groupbody"]/>,
      "summary":"Create a group",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]

},


{
  "path": "/group/{groupName}/user/{userName}",
  "description": "Add a user to a group",
  "operations" : [
    {
      "method":"POST",
      "nickname":"addAUserToGroup",
      "responseClass":"NuxeoPrincipal",
      "type":"Document",
      <@params names = ["groupname","username"]/>,
      "summary":"Add a user to a group",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]
}


</@block>

<@block name="models">
  <#include "views/doc/datatypes/nuxeoprincipal.ftl"/>
</@block>
</@extends>