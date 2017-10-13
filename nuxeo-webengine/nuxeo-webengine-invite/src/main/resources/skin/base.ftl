<!doctype html>
<html>
<head>
  <title>
     <@block name="title">
     WebEngine Project
     </@block>
  </title>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <link href="${skinPath}/css/site.css" rel="stylesheet">
  <link href="${skinPath}/css/nuxeo-user-registration-embedded.css" rel="stylesheet">
  <link rel="shortcut icon" href="${skinPath}/img/favicon.ico"/>
  <script src="${skinPath}/js/placeholders.min.js"></script>
  <@block name="stylesheets" />
  <@block name="header_scripts" />
</head>

<body>

  <div class="container">
    <div class="registrationBox">
      <div class="logo"></div>
      <@block name="content">The Content</@block>
    </div>
  </div>

</body>
</html>