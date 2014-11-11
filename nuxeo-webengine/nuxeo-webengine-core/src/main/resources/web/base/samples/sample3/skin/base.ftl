<!-- Base template that defines the site layout -->
<html>
  <head>
    <title><@block name="title"/></title>
  </head>
  <body>
    <table width="100%" border="1">
      <tr>
        <td><@block name="header">Header</@block></td>
      </tr>
      <tr>
        <td><@block name="content">Content</@block></td>
      </tr>
      <tr>
        <td><@block name="footer">Footer</@block></td>
      </tr>
    </table>
  </body>
</html>

