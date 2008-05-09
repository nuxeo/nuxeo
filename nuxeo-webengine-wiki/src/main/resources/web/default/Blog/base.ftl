<html>

<head>
    <title>${root.title} : ${this.title}</title>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
<link rel="stylesheet" href="/nuxeo/site/files/resources/css/webengine.css" type="text/css" media="screen" charset="utf-8">
<link rel="stylesheet" href="/nuxeo/site/files/resources/css/blog.css" type="text/css" media="screen" charset="utf-8">
<script src="/nuxeo/site/files/resources/script/jquery/jquery.js"></script>
  <link rel="stylesheet" href="/nuxeo/site/files/resources/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
  <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.tabs.js"></script>
<base href="${this.docURL}">

</head>

<body>

<div id="wrap">
    <div id="header">
      <div class="searchBox">
      <form action="${root.docURL}@@search" method="get" accept-charset="utf-8">
        <input class="complete" type="text" name="q" id="q" autosave="com.mysite" results="5" value="Search">
        <input type="hidden" name="p" value="${root.path}">
      </form>
     </div>  
       <h1><a href="/nuxeo/site/${root.name}">${root.title}</a></h1>
    </div>
    <div id="main-wrapper">
      <div id="main">
        <div class="main-content">
          <@block name="content">
          ##This is the content block##
          </@block>
        </div>  
      </div>

      <div id="sidebar">
          <#include "/default/includes/sidebar.ftl"/>
      </div>
    </div>
    <div id="footer">
       <p>Last modified by ${this.author} @ ${this.dublincore.modified?datetime}</p>
       <p>&copy; 2000-2008 <a href="http://www.nuxeo.com/en/">Nuxeo</a>.</p>
    </div>
    
</div>

</body>
</html>