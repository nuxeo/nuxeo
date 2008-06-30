

<h1>User Administration</h1>

<br/>

<a href="${appPath}/users/create_user">Add a user</a>
<a href="${appPath}/users/create_group">Add a group</a>

<br/>
<br/>

<form method="POST" action="${Context.URL}" accept-charset="utf-8">
  <input type="text" name="query" value=""/>
  <input type="submit" value="Search"/>
</form>

<br/>

<#if users>
<table>
<thead>
	<tr>
		<th>Username</th>
		<th>First name</th>
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
