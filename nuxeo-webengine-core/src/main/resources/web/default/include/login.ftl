
<!-- Login Form -->
<form action="${Context.URL}" method="POST">
<table cellpadding="4" cellspacing="1">
  <tr>
    <td>Username:</td>
    <td><input name="userid" type="text"></td>
  </tr>
  <tr>
    <td>Password:</td>
    <td><input name="password" type="password"></td>
  </tr>
  <tr bgcolor="#ffffff" align="right">
    <td colspan="2">
      <input name="nuxeo_login" type="submit" value="Sign In"/>
    </td>
  </tr>
  <#if Request.getParameter("failed") == "true">
  <tr bgcolor="#ffffff" align="center">
    <td colspan="2"><font color="red">Authentication Failed!</font></td>
  </tr>
  </#if>
</table>
</form>

