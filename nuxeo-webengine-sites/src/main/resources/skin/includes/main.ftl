<#macro main>
  
  <link rel="stylesheet" href="${skinPath}/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
  <link rel="shortcut icon" href="${skinPath}/image/favicon.gif" />
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.tabs.js"></script>
  <!-- tinyMCE -->
<script type="text/javascript" src="${skinPath}/script/fckeditor/fckeditor.js"></script>
<!-- end tinyMCE -->
  
  <script>
  
  	var oFCKeditor = null;
  
  	$(document).ready(function(){
      $("#webpage-actions > ul").tabs({
		show: function(event, ui){
			
			///nuxeo/site/skin/sites/script/fckeditor/fckeditor.js
			oFCKeditor = new FCKeditor( 'richtextEditor' ) ;
			oFCKeditor.BasePath = "${skinPath}/script/fckeditor/" ;
			oFCKeditor.Width = "100%";
			oFCKeditor.Height = "400";
			oFCKeditor.ReplaceTextarea();			
			
		}
	});
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