<@extends src="base.ftl">
<@block name="header"><h1><a href ="${basePath}"><#if user>User Details<#else>User creation</#if></a></h1></@block>
<@block name="content">

<form method="POST" action="${appPath}/users/save_user" accept-charset="utf-8">
<table>
    <tbody>
        <tr>
            <td>Username</td>
            <td><#if user><input type="hidden" name="username" value="${user.name}"/>${user.name}<#else><input type="text" name="username" value=""/></#if></td>
        </tr>
        <tr>
            <td>Password</td>
            <td><input type="password" name="password" value="<#if user>${user.password}</#if>"/></td>
        </tr>
        <tr>
            <td>First name</td>
            <td><input type="text" name="firstName" value="<#if user>${user.firstName}</#if>"/></td>
        </tr>
        <tr>
            <td>Last name</td>
            <td><input type="text" name="lastName" value="<#if user>${user.lastName}</#if>"/></td>
        </tr>
        <tr>
            <td>Groups</td>
            <td>
                <select multiple="multiple" name="groups" size="4">
                <#list allGroups as group>
                    <#if user>
                    <option value="${group.name}" <#if user.groups?seq_contains(group.name)>selected="selected"</#if>>${group.name}</option>
                    <#else>
                    <option value="${group.name}">${group.name}</option>
                    </#if>
                </#list>
                </select>
            </td>
        </tr>
    </tbody>
</table>
  <input type="submit" value="Save"/>
</form>

</@block>
</@extends>
