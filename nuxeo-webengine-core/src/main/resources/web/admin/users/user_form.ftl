<@extends src="base.ftl">
<@block name="header"><h1><a href ="${appPath}/users"><#if user>User Details<#else>User creation</#if></a></h1></@block>


<@block name="toolbox">
  <div class="sideblock contextual">
    <h3>Toolbox</h3>
    <div class="sideblock-content">
      <ul>
        <li><a href="${appPath}/users/create_user">Create an user</a></li>
        <li><a href="${appPath}/users/create_group">Create a group</a></li>
        <li><a href="${appPath}/users/delete_user">Delete user</a></li>
      </ul>
    </div>
  </div>
</@block>

<@block name="content">

<#if user><h1>${user.name}</h1></#if>
<div>
<form method="POST" action="${appPath}/users/save_user" accept-charset="utf-8">
<table>
    <tbody>
        <#if user><input type="hidden" name="username" value="${user.name}"/><#else>
        <tr>
            <td>Username</td>
            <td><input type="text" name="username" value=""/></td>
        </tr>
        </#if>
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
                <select multiple="multiple" name="groups" size="8">
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
        <tr><td colspan="2"><input type="submit" value="Save"/></td></tr>
    </tbody>
</table>  
</form>

</div>

</@block>
</@extends>
