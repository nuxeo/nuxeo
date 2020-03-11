<!-- Login Form -->
<form action="${Context.loginPath}" method="POST">
<table cellpadding="4" cellspacing="1">
  <tr>
    <td>Username:</td>
    <td><input name="username" type="text"></td>
  </tr>
  <tr>
    <td>Password:</td>
    <td><input name="password" type="password"></td>
  </tr>
  <tr align="right">
    <td colspan="2">
      <input type="submit" value="Sign In"/>
    </td>
  </tr>
  <#if Context.getProperty("failed") == "true">
  <tr align="center">
    <td colspan="2"><font color="red">Authentication Failed!</font></td>
  </tr>
  </#if>
</table>
</form>
