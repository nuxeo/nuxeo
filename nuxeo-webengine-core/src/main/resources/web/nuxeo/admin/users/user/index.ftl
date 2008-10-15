<@extends src="base.ftl">
<@block name="header"><h1><a href ="${This.previous.path}"><#if user>User Details<#else>User creation</#if></a></h1></@block>


<@block name="content">

<div id="mainContentBox">

<#if user><h1>${user.name}</h1></#if>
<div>
<form method="POST" action="${This.path}/@put" accept-charset="utf-8">
<table class="formFill">
    <tbody>
        <#if user><input type="hidden" name="username" value="${user.name}"/><#else>
        <tr>
            <td>Username</td>
            <td><input type="text" name="username" value=""/></td>
        </tr>
        </#if>
        <tr>
            <td class="formLabel">Password</td>
            <td class="formValue"><input type="password" name="password" value="<#if user>${user.password}</#if>"/></td>
        </tr>
        <tr>
            <td class="formLabel">First name</td>
            <td class="formValue"><input type="text" name="firstName" value="<#if user>${user.firstName}</#if>"/></td>
        </tr>
        <tr>
            <td class="formLabel">Last name</td>
            <td class="formValue"><input type="text" name="lastName" value="<#if user>${user.lastName}</#if>"/></td>
        </tr>
        <tr>
            <td class="formLabel">Groups</td>
            <td class="formValue">
                <select multiple="multiple" name="groups" size="8">
                <#list This.previous.groups as group>
                    <#if user>
                    <option value="${group.name}" <#if user.groups?seq_contains(group.name)>selected="selected"</#if>>${group.name}</option>
                    <#else>
                    <option value="${group.name}">${group.name}</option>
                    </#if>
                </#list>
                </select>
            </td>
        </tr>
        <tr><td/><td><input type="submit" value="Save"/></td></tr>
    </tbody>
</table>  
</form>

</div>
</div>
</@block>
</@extends>
