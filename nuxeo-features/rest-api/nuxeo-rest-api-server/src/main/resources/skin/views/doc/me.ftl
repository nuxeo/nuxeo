<#include "views/doc/macros.ftl"/>
<@extends src="baseapi.ftl">
<@block name="apis">
 {
  "path": "/me",
  "description": "Operation on logged in user",
  "operations" : [
    {
      "method":"GET",
      "nickname":"getLoggedInUser",
      "type":"user",
      "summary":"Get the logged in user",
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]
},

{
  "path": "/me/changepassword",
  "description": "Operation on logged in user",
  "operations" : [
    {
      "method":"PUT",
      "nickname":"changePassword",
      "type":"user",
      "summary":"Change the logged in user's password",
      <@params names = ["passwordbody"]/>,
      "notes": "",
      <#include "views/doc/errorresponses.ftl"/>
    }

  ]

}


</@block>

<@block name="models">
  <#include "views/doc/datatypes/user.ftl"/>,
  <#include "views/doc/datatypes/password.ftl"/>
</@block>
</@extends>
