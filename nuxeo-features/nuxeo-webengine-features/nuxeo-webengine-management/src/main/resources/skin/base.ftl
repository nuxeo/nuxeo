<html>
<head>
  <title>
    <@block name="title">Nuxeo Management</@block>
  </title>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>

  <link rel="shortcut icon" href="${skinPath}/images/favicon.png"></link>
  <link rel="stylesheet" href="${skinPath}/css/studio_style.css" type="text/css" media="screen" charset="utf-8"/>

  <@block name="stylesheets" />

   <script type="text/javascript" src="${skinPath}/script/jquery/jquery.js"></script>
   <script type="text/javascript" src="${skinPath}/script/jquery/cookie.js"></script>

   <@block name="header_scripts" />

</head>

<body>

<h1>Nuxeo Server Management</h1>

<table width="100%" cellpadding="0" cellspacing="0">
  <tr valign="middle">
    <td class="header">
      <@block name="header">
        <div class="logo">
          <a href="${Root.path}"></a>
        </div>
        <div style="clear:both;"></div>
      </@block>
    </td>
  </tr> 
  <tr valign="top" align="left">
    <td style="padding: 25px 50px;">
      <table width="100%" cellpadding="0" cellspacing="0">
        <tr valign="top" align="left">
          <td class="middle-content-block">
          <@block name="content"/>
          </td>
          <td class="right-content-block">

          <@block name="toolbox"/>

          <@block name="links">
            <ul><h3>Management</h3>
               <li>Get your <a href="${This.context.modulePath}/principal">principal</a></li>
               <li>List resources <a href="${This.context.modulePath}/locks">locks</a></li>
               <li>List job <a href="${This.context.modulePath}/queues">queues</a></li>
               <li>List server <a href="${This.context.modulePath}/statuses">statuses</a></li>
            </ul>
          </@block>

          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr valign="middle" align="center">
    <td class="footer">
      <@block name="footer">
        <div style="clear:both;"></div>
      </@block>
    </td>
  </tr>
</table>



</body>
</html>