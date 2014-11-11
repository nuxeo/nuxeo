<@extends src="base.ftl">
<@block name="header"><h1><a href="${basePath}">Create a new space</a></h1></@block>
<@block name="content">
  <div id="mainContentBox">
    <form action="${This.path}/@createSpace" method="POST">
      <table class="formFill">
          <tbody>
              <tr>
                  <td>Name:</td>
                  <td><input type="text" name="name" value="" size="40"></td>
              </tr>
              </tr>
              <tr>
                  <td>Title:</td>
                  <td><input type="text" name="dc:title" value="" size="40"/></td>
              </tr>
              <tr>
                  <td colspan="2">Description</td>
              </tr>
              <tr>
                  <td colspan="2"><textarea name="dc:description" cols="54"></textarea></td>
              </tr>
              <tr>
                <td colspan="2" align="right"><input type="submit" value="Create"/></td>
              </tr>
          </tbody>
      </table>
    </form>
  </div>
  </@block>
</@extends>
