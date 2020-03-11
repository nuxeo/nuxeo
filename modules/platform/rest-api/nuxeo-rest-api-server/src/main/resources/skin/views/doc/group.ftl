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
      "type":"group",
      <@params names = ["groupname"]/>,
      "summary":"Get a group by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"PUT",
      "nickname":"updateGroupByName",
      "type":"group",
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
},
{
  "path": "/group/{groupName}/@users",
  "description": "Group members adapter",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getGroupMembers",
      "type":"userList",
      <@params names = ["groupname"]/>,
      "summary":"Get the user members of a group",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]
},
{
  "path": "/group/{groupName}/@groups",
  "description": "Group members group adapter",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getGroupMembersGroup",
      "type":"groupList",
      <@params names = ["groupname"]/>,
      "summary":"Get the group members of a group",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]
},
{
  "path": "/group/search",
  "description": "Operation on groups",
  "operations" : [
    {
      "method":"GET",
      "nickname":"searchGroup",
      "type":"groups",
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
      "type":"group",
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
      "type":"user",
      <@params names = ["groupname","username"]/>,
      "summary":"Add a user to a group",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }
  ]
}


</@block>

<@block name="models">
  <#include "views/doc/datatypes/user.ftl"/>
</@block>
</@extends>