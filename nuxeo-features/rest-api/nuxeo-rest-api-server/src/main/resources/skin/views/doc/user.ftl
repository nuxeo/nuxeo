<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
 {
  "path": "/user/{userName}",
  "description": "Operation on users",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getUserByName",
      "type":"user",
      <@params names = ["username"]/>,
      "summary":"Get a user by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"PUT",
      "nickname":"updateUserByName",
      "type":"user",
      <@params names = ["username","userbody"]/>,
      "summary":"Update a user by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    },
    {
      "method":"DELETE",
      "nickname":"deleteUserByName",
      "type":"user",
      <@params names = ["username"]/>,
      "summary":"Delete a user by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]
},

{
  "path": "/user/search",
  "description": "Operation on users",
  "operations" : [
    {
      "method":"GET",
      "nickname":"searchUser",
      "type":"users",
      <@params names = ["userquery"]/>,
      "summary":"Get a user by its name",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]

}

,

{
  "path": "/user",
  "description": "Operation on users",
  "operations" : [
    {
      "method":"POST",
      "nickname":"createUser",
      "type":"user",
      <@params names = ["userbody"]/>,
      "summary":"Create a user",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]

},

{
  "path": "/user/{userName}/group/{groupName}",
  "description": "Add a group to a user",
  "operations" : [
    {
      "method":"POST",
      "nickname":"addAGroupToUser",
      "type":"user",
      <@params names = ["username","groupname"]/>,
      "summary":"Add a group to a user",
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
