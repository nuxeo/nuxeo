<@theme>
<html>
  <head>
   <@block name="header_scripts">
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8" />
    <script type="text/javascript" src="${skinPath}/script/loading.js"></script>

  </@block>
    <title><@block name="title"/></title>
     <link rel="stylesheet" href="${skinPath}/css/univers.css" type="text/css" media="screen" charset="utf-8" />
  </head>
  <body>




    <table width="100%" border="0">
      <tr>
        <td><@block name="header"></@block></td>
      </tr>
      <tr>
        <td><@block name="content">Content</@block></td>
      </tr>
    </table>
  </body>
</html>
</@theme>