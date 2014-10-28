<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <title>
     <@block name="title">
     Nuxeo - EasyShare
     </@block>
  </title>
  <meta name="description" content="Nuxeo Easy Share Folder">
  <meta name="viewport" content="width=device-width">
  
  <script src="../javascript/jquery-1.7.1.min.js" charset="utf-8"></script>
  <script type="text/javascript" src="//www.nuxeo.com/wp-content/themes/nuxeo.com_wp/js/xnav_get.js" charset="utf-8"></script>
  
  <link href='//fonts.googleapis.com/css?family=Open+Sans' rel='stylesheet' type='text/css'>
  <link rel="stylesheet" href="${skinPath}/css/nuxeo-easyshare-embedded.css">
  <link rel="stylesheet" href="${skinPath}/css/normalize.css">
  <link rel="stylesheet" href="${skinPath}/css/site.css">
  <link rel="shortcut icon" href="${skinPath}/img/favicon.ico" />
</head>

<body>
   
 

  <!--header>
    <@block name="header">Nuxeo - Easy Share  </@block>
  </header -->

  <section>
    <div class="wrapper">
      <div class="nuxeo-logo">
        <img src="${skinPath}/img/nuxeo_logo.png" />
      </div>
      <main class="share-box">
        <@block name="content">The Content</@block>
      </main>
    </div>
  </section>
  
</body>
</html>
