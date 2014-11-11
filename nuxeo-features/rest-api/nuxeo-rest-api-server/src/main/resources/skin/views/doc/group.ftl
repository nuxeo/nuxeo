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
      "type":"NuxeoGroup",
      <@params names = ["groupname"]/>,
      "summary":"Get a group by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"PUT",
      "nickname":"updateGroupByName",
      "type":"NuxeoGroup",
      <@params names = ["groupname","groupbody"]/>,
      "summary":"Update a group by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"DELETE",
      "nickname":"deleteGroupByName",
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
      "type":"NuxeoGroupList",
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
      "type":"NuxeoGroup",
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
      "type":"NuxeoPrincipal",
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