<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <title>
  <@block name="title">
    ${Context.getMessage("easyshare.label.title")}
  </@block>
  </title>
  <meta name="description" content="${Context.getMessage("easyshare.label.description")}">
  <meta name="viewport" content="width=device-width">

  <script src="${skinPath}/javascript/jquery-1.7.1.min.js" charset="utf-8"></script>

  <link href='//fonts.googleapis.com/css?family=Open+Sans' rel='stylesheet' type='text/css'>
  <link rel="stylesheet" href="${skinPath}/css/nuxeo-easyshare-embedded.css">
  <link rel="stylesheet" href="${skinPath}/css/normalize.css">
  <link rel="stylesheet" href="${skinPath}/css/site.css">
  <link rel="stylesheet" href="/nuxeo/css/nuxeo-easyshare-override.css">
  <link rel="shortcut icon" href="${skinPath}/img/favicon.ico"/>
</head>

<body>

<section>
  <div class="header">
    <div class="nuxeo-logo"></div>
  </div>
  <div class="wrapper">
    <main class="share-box">
    <@block name="content">${Context.getMessage("easyshare.label.content")}</@block>
    </main>
  </div>
</section>

</body>
</html>
