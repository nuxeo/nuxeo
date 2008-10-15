<@extends src="base.ftl">
<@block name="header"><h1><a href ="${appPath}/users"><#if group>Group Details<#else>Group creation</#if></a></h1></@block>

<@block name="content">

<#if group>

<div id="mainContentBox">
<h1>${group.name}</h1>
</#if>

<h2>Members</h2>

<form method="POST" action="${appPath}/users/save_group" accept-charset="utf-8">
        <#if group>
          <input type="hidden" name="groupName" value="${group.name}"/>
          <#list usersGroup as user>
            <li><a href="${appPath}/users/user/${user}">${user}</a>
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
