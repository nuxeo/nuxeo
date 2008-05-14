<html>

<head>
    <title>${Root.document.title} :: ${This.document.title}</title>
    
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
    
<link rel="stylesheet" href="/nuxeo/site/files/resources/css/webengine.css" type="text/css" media="screen" charset="utf-8">
<link rel="stylesheet" href="/nuxeo/site/files/resources/css/wiki.css" type="text/css" media="screen" charset="utf-8">
<script src="/nuxeo/site/files/resources/script/jquery/jquery.js"></script>
  <link rel="stylesheet" href="/nuxeo/site/files/resources/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
  <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="/nuxeo/site/files/resources/script/jquery/ui/ui.tabs.js"></script>
<base href="${This.urlPath}">

<script>

$.fn.search = function() {
	return $(this).focus(function() {
		if( $(this).value == This.defaultValue ) {
			$(this).value = "";
		}
	}).blur(function() {
		if( !This.value.length ) {
			This.value = This.defaultValue;
		}
	});
};



</script>

</head>

<body>

<div id="wrap">
    <div id="header">
       <div class="searchBox">
      <form action="${Root.urlPath}@@search" method="get" accept-charset="utf-8">
        <input type="text" name="q" id="q" autosave="com.mysite" results="5" value="Search">
        <input type="hidden" name="p" value="${Root.repositoryPath}">
      </form>
     </div>  
       <h1><a href="${Root.urlPath}">${Root.document.title}</a></h1>
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
          <#include "includes/sidebar.ftl"/>
      </div>
    </div>
    <div id="footer">
       <p>Last modified by ${Document.dublincore.creator} @ ${Document.dublincore.modified?datetime}</p>
       <p>&copy; 2000-2008 <a href="http://www.nuxeo.com/en/">Nuxeo</a>.</p>
    </div>
    
</div>

</body>
</html>