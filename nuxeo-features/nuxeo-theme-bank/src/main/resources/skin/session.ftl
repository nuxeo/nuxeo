<@extends src="base.ftl">

  <@block name="title">
    Login
  </@block>

  <@block name="content">

  <div class="window">

<#if Context.principal>

<h1>Log out</h1>
<p>You are logged in as: <b>${Context.principal}</b></p>

<form action="${Root.path}/session/@@login" method="POST">
  <div>
      <input type="submit" style="font-weight: bold" value="Log out"/>
  </div>
</form>

<#else>

  <h1>Log in
  <#if Context.getProperty("failed") == "true">
    <span style="float: right; color: red; background-color: #ffc; font-weight: bold;">Authentication Failed!</span>
  </#if>
  </h1>

  <form action="${Root.path}/session/@@login" method="POST">
<table cellpadding="4" cellspacing="1">
  <tr>
    <td>Username:</td>
    <td><input name="username" type="text"></td>
  </tr>
  <tr>
    <td>Password:</td>
    <td><input name="password" type="password"></td>
  </tr>
  <tr>
    <td style="text-align: right" colspan="2">
      <input type="submit" style="font-weight: bold" value="Log in"/>
    </td>
  </tr>

</table>
</form>

</#if>
  </div>

  </@block>

</@extends>
