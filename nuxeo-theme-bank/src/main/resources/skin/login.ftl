<@extends src="base.ftl">

  <@block name="title">
    Login
  </@block>

  <@block name="content">

<#if (Context.principal.isAnonymous())>

  <h1>Log in</h1>
  <form action="${Root.path}/@@login" method="POST">
<table cellpadding="4" cellspacing="1">
  <tr>
    <td>Username:</td>
    <td><input name="username" type="text"></td>
  </tr>
  <tr>
    <td>Password:</td>
    <td><input name="password" type="password"></td>
  </tr>
  <tr align="right">
    <td colspan="2">
      <input type="submit" value="Log in"/>
    </td>
  </tr>
  <#if Context.getProperty("failed") == "true">
  <tr align="center">
    <td colspan="2"><font color="red">Authentication Failed!</font></td>
  </tr>
  </#if>
</table>
</form>

<#else>

<h1>Log out</h1>
<p>You are logged in as: <b>${Context.principal}</b></p>

<form action="${Root.path}/@@login" method="POST">
  <div>
    <input type="submit" value="Log out"/>
  </div>
</form>

</#if>


  </@block>

</@extends>