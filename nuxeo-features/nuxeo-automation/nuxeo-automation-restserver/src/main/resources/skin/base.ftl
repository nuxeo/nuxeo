<html>
<head>
  <title>
     <@block name="title">
     WebEngine Project
     </@block>
  </title>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
  <link rel="stylesheet" href="${skinPath}/css/site.css" type="text/css" media="screen" charset="utf-8">
  <link rel="shortcut icon" href="${skinPath}/image/favicon.gif" />
  <@block name="stylesheets" />
  <@block name="header_scripts" />
</head>

<body style="margin:0px 0px 0px 0px;">

  <table class="main">
    <tr>
      <td>
        <table class="header">
            <tr>
            <td><img src="${skinPath}/img/logo.png"</td>
            <td align="right"><@block name="header">The Header</@block></td>
            </tr>
        </table>
      </td>
    </tr>
    <tr height="98%">
      <td valign="top"><@block name="content">The Content</@block></td>
    </tr>
  </table/>

</body>
</html>
