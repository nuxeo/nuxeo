
<!-- Login Form -->
<form action="${Context.URL}" method="POST">
<table cellspacing="0" cellpadding="0">
  <tr>
    <td bgcolor="#efefef">
      <table cellpadding="4" cellspacing="1">
        <tr bgcolor="#ffffff">
          <td>Username:</td>
          <td><input name="userid" type="text"></td>
        </tr>
        <tr bgcolor="#ffffff">
          <td>Password:</td>
          <td><input name="password" type="password"></td>
        </tr>
        <tr bgcolor="#ffffff" align="right">
          <td colspan="2">
            <input type="reset"/>
            <input name="nuxeo@@login" type="submit" value="Sign In"/>
          </td>
        </tr>
        <#if Request.getParameter("failed") == "true">
        <tr bgcolor="#ffffff" align="center">
          <td colspan="2"><font color="red">Authentication Failed!</font></td>
        </tr>
        </#if>
      </table>
    </td>
  </tr>
</table>
</form>