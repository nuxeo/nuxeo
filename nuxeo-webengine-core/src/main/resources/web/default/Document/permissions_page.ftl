<#assign perms = script("common/listpermissions.groovy") />
<h4>Access Rights</h4>
<#if perms>
<table>
    <thead>
        <tr>
            <th>User or group</th>
            <th>Granted permissions</th>
            <th>Denied permissions</th>
        </tr>
    </thead>
    <#list perms as perm>
    <tr>
        <td>${perm.name}</td>
        <#if perm.granted>
            <td>${perm.permission}</td>
            <td></td>
        <#else>
            <td></td>
            <td>${perm.permission}</td>
        </#if>
    </tr>
    </#list>
</table>
</#if>
<form method="POST" action="${This.urlPath}/@@addpermission">
    Add a permission:
    <select name="action" size="1">
        <option value="grant">Grant</option>
        <option value="deny">Deny</option>
    </select>&nbsp;
    <select name="permission" size="1">
        <option value="Read">Read</option>
        <option value="Write">Write</option>
        <option value="Everything">Everything</option>
    </select>
    to user or group <input type="text" name="user" value="">
    <input type="submit" value="Add permission">
</form>
