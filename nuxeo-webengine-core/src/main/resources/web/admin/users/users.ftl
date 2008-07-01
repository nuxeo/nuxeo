<@extends src="base.ftl">
<@block name="header"><h1><a href ="${appPath}/users">User Administration</a></h1></@block>

<@block name="toolbox">
  <div class="sideblock contextual">
    <h3>Toolbox</h3>
    <div class="sideblock-content">
      <ul>
        <li><a href="${appPath}/users/create_user">Create an user</a></li>
        <li><a href="${appPath}/users/create_group">Create a group</a></li>
      </ul>
    </div>
  </div>
</@block>

<@block name="content">

<p>
<div>Search users:</div>
</p>
<form method="POST" action="${Context.URL}" accept-charset="utf-8">
  <input type="text" name="query" value=""/>
  <input type="submit" value="Search"/>
</form>

<p>
<div>Search groups:</div>
</p>
<form method="POST" action="${Context.URL}" accept-charset="utf-8">
  <input type="text" name="gquery" value=""/>
  <input type="submit" value="Search"/>
</form>

<br/>

<#if users>
<table>
<thead>
	<tr>
		<th>User Name</th>
		<th>First Name</th>
		<th>Last Name</th>
	</tr>
</thead>
<tbody>
<#list users as user>
	<tr>
		<td><a href="${appPath}/users/user/${user}">${user}<a/></td>
		<td>${user.firstName}</td>
		<td>${user.lastName}</td>
	</tr>
</#list>
</tbody>
</table>
</#if>


<#if groups>
<table>
<thead>
  <tr>
    <th>Group Name</th>
  </tr>
</thead>
<tbody>
<#list groups as user>
  <tr>
    <td><a href="${appPath}/users/group/${user}">${user}<a/></td>
  </tr>
</#list>
</tbody>
</table>
</#if>

</@block>
</@extends>

