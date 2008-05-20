<html>

  <head>
    <title>Wiki : ${Root.document['dc:title']}</title>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    <link rel="stylesheet" href="/nuxeo/site/files/resources/css/webengine.css" type="text/css" media="screen" charset="utf-8">
    <script src="/nuxeo/site/files/resources/script/jquery/jquery.js"></script>
    <link rel="stylesheet" href="/nuxeo/site/files/resources/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.base.js"></script>
    <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.tabs.js"></script>
    <link rel="shortcut icon" href="/nuxeo/site/files/resources/image/favicon.gif" />
  </head>

<body>

<div id="wrap">
    <div id="header">
       <h1><a href="/nuxeo/site/${Root.document.name}">${Root.document.title}</a></h1>
    </div>
    <div id="main-wrapper">
      <div id="main">
        <div class="main-content">
          <p>
            <@block name="message"/>
          </p>
          <@block name="content">
          ##This is the content block##
          </@block>
        </div>  
      </div>

      <div id="sidebar">
      </div>
    </div>
    <div id="footer">
       <p>Last modified by ${This.document.creator} @ ${Document.dublincore.modified?datetime}</p>
       <p>&copy; 2000-2008 <a href="http://www.nuxeo.com/en/">Nuxeo</a>.</p>
    </div>
    
</div>

</body>
</html>
