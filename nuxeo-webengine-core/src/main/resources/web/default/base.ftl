<html>

  <head>    
    <title><@block name="windowTitle">WebEngine</@block></title>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    <link rel="shortcut icon" href="/nuxeo/site/files/resources/image/favicon.gif" />
    <@block name="stylesheets" />
    <@block name="header_scripts" />    
  </head>

<body>
<h1>${This.document.title}</h1>

<p>
<@block name="message"/>
</p>

<@block name="content"/>

<hr>
engine : ${env.engine} ${env.version}

</body>
</html>
