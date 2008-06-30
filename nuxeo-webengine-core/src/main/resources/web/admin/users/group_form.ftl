<@extends src="base.ftl">
<@block name="header"><h1><a href ="${appPath}">Group creation</a></h1></@block>
<@block name="content">

<br/>
<form method="POST" action="${appPath}/users/save_group" accept-charset="utf-8">
<table>
    <tbody>
        <tr>
            <td>Name</td>
            <td><input type="text" name="groupName" value=""/></td>
        </tr>
    </tbody>
</table>
  <input type="submit" value="Save"/>
</form>

</@block>
</@extends>
