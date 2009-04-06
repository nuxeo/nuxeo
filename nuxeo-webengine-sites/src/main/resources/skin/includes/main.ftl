<#macro main>
  
  <link rel="stylesheet" href="${skinPath}/script/jquery/ui/themes/flora/flora.all.css" type="text/css" media="screen" title="Flora (Default)">
  <link rel="shortcut icon" href="${skinPath}/image/favicon.gif" />
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.base.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery/ui/ui.tabs.js"></script>
  	<!-- tinyMCE -->
	<script type="text/javascript" src="${skinPath}/script/tiny_mce/tiny_mce.js"></script>
	<!-- end tinyMCE -->
  <script>
 
  
  	$(document).ready(function(){
      $("#webpage-actions > ul").tabs({
		show: function(event, ui){
			
			tinyMCE.init({
			  // General options
			  mode : "exact",
			  elements : "richtextEditor",
			  theme : "advanced",
			  plugins : "safari,spellchecker,pagebreak,style,layer,table,save,advhr,advimage,advlink,emotions,iespell,inlinepopups,insertdatetime,preview,media,searchreplace,print,contextmenu,paste,directionality,fullscreen,noneditable,visualchars,nonbreaking,xhtmlxtras,template,imagemanager,filemanager",
			
			  // Theme options
			  theme_advanced_buttons1 : "save,newdocument,|,bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,|,styleselect,formatselect,fontselect,fontsizeselect",
			  theme_advanced_buttons2 : "cut,copy,paste,pastetext,pasteword,|,search,replace,|,bullist,numlist,|,outdent,indent,blockquote,|,undo,redo,|,link,unlink,anchor,image,cleanup,help,code,|,insertdate,inserttime,preview,|,forecolor,backcolor",
			  theme_advanced_buttons3 : "tablecontrols,|,hr,removeformat,visualaid,|,sub,sup,|,charmap,emotions,iespell,media,advhr,|,print,|,ltr,rtl,|,fullscreen",
			  theme_advanced_buttons4 : "insertlayer,moveforward,movebackward,absolute,|,styleprops,spellchecker,|,cite,abbr,acronym,del,ins,attribs,|,visualchars,nonbreaking,template,blockquote,pagebreak,|,insertfile,insertimage",
			  theme_advanced_toolbar_location : "top",
			  theme_advanced_toolbar_align : "left",
			  theme_advanced_statusbar_location : "bottom",
			  theme_advanced_resizing : true
			
			});
			
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