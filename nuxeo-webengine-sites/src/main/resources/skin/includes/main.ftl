<#macro main>
  
  <link rel="stylesheet" href="${skinPath}/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
  <link rel="shortcut icon" href="${skinPath}/image/favicon.gif" />

<script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.base.js"></script>
<script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.tabs.js"></script>
<script src="${skinPath}/script/markitup/jquery.markitup.pack.js"></script>
<script src="${skinPath}/script/markitup/sets/wiki/set.js"></script>
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/skins/markitup/style.css" />
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/sets/wiki/style.css" />
<!-- end markitup -->
<!-- tinyMCE -->
<script type="text/javascript" src="../../../${skinPath}/script/tiny_mce/tiny_mce.js"></script>
<script type="text/javascript" src="../../../${skinPath}/script/tiny_mce/langs/en.js"></script>
<script type="text/javascript" src="../../../${skinPath}/script/tiny_mce/themes/simple/editor_template.js"></script>
<!-- end tinyMCE -->

  <script>
  
    $(document).ready(function(){
    $("#webpage-actions > ul").tabs({
	show : function(event,id)
	{
		
	}}
	);
    });
  </script>
  
  <div id="webpage-actions">
    <ul>
      <#list This.getLinks("SITE_ACTIONS") as link>
          <li><a href="${link.getCode(This)}" title="${link.id}"><span>${Context.getMessage(link.id)}</span></a></li>
      </#list>
    </ul>
  </div>

</#macro>