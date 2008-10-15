<@extends src="base.ftl">
<@block name="header"><h1><a href ="${This.previous.path}"><#if group>Group Details<#else>Group creation</#if></a></h1></@block>

<@block name="content">

<#if group>

<div id="mainContentBox">
<h1>${group.name}</h1>
</#if>

<h2>Members</h2>

<form method="POST" action="${This.path}/@put" accept-charset="utf-8">
        <#if group>
          <input type="hidden" name="groupName" value="${group.name}"/>
          <#list usersGroup as user>
            <li><a href="${This.previous.path}/user/${user}">${user}</a>
          </#list>
        <#else>
<table>
    <tbody>
        <tr>
            <td>Name</td> 
            <td><input type="text" name="groupName" value=""/></td>
        </tr>
        <tr>
          <td colspan="2"><input type="submit" value="Save"/></td>
        </tr>
    </tbody>
</table>
</#if>
</form>
</div>

</@block>
</@extends>
