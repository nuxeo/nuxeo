<html>
  <head>
    <title>Preview of TITRE DU DOCUMENT</title>
    <link rel="stylesheet" href="/nuxeo/site/files/resources/css/webengine.css" type="text/css" media="screen" charset="utf-8">
    <link rel="stylesheet" href="/nuxeo/site/files/resources/css/preview.css" type="text/css" media="screen" charset="utf-8">
    <!-- DETECT if in wiki : call wiki.css
    or if in blog, blog.css -->
    </head>
  <body>
    <div id="main">
      <div class="main-content">
        <h1>Titre du document</h1>
        <@wiki>${Request.getParameter('content')}</@wiki> 
      </div>
      <div class="closeWindow">
        <a href="javascript:self.close()">Close this window</a>
      </div>
    </div>
  </body>
</html>
