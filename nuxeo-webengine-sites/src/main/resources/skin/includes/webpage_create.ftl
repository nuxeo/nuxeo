<#macro webPageCreate>
<!-- markitup -->
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

<script type="text/javascript">
	function onSelectRadio(obj) {

		if (obj.id == "wikitext") {
			var wiki = document.getElementById('wikitextArea');
			if (wiki) {
				wiki.style.display = 'block';
				wiki.style.zIndex = '10000';
			} else {
				alert('Oups problem getting component !');
			}

			var rich = document.getElementById('richtextArea');
			if (rich) {
				rich.style.display = 'none';
				rich.style.zIndex = '1';
			} else {
				alert('Oups problem getting component !');
			}
		}

		else {
			var rich = document.getElementById('richtextArea');
			if (rich) {
				rich.style.display = 'block';
				rich.style.zIndex = '10000';
			} else {
				alert('Oups problem getting component !');
			}

			var wiki = document.getElementById('wikitextArea');
			if (wiki) {
				wiki.style.display = 'none';
				wiki.style.zIndex = '10000';
			} else {
				alert('Oups problem getting component !');
			}
		}
	}

	function isTitleSet() {
		var title = document.getElementById('titleInput');
		if (title) {
			if (title.value == "") {
				alert("Please insert a valid title.");
				return false;
			}
		}

		return true;
	}
</script>

<form name="pageCreate" method="POST" onsubmit="return isTitleSet();"
  action="${This.path}/createWebPage" accept-charset="utf-8">
<table class="createWebPage">
  <tbody>
    <tr>
      <td width="30%"></td>
      <td width="70%"></td>
    </tr>
    <tr>
      <td>${Context.getMessage("label.page.title")}</td>
      <td><input type="text" id="titleInput" name="title" value="" /></td>
    </tr>
    <tr>
      <td>${Context.getMessage("label.page.description")}</td>
      <td><textarea name="description"></textarea></td>
    </tr>
    <tr>
      <td>${Context.getMessage("label.page.format")}</td>
      <td><input type="radio" onclick="onSelectRadio(this);"
        name="isRichtext" id="wikitext" value="false" checked="true" />${Context.getMessage("label.page.format.wikitext")}
      <input type="radio" onclick="onSelectRadio(this);" name="isRichtext"
        id="richtext" value="true" />${Context.getMessage("label.page.format.richtext")}
      </td>
    </tr>
    <tr>
      <td colspan="2">
      <div id="wikitextArea"><textarea name="wikitextEditor"
        cols="60" rows="20" id="wiki_editor"></textarea></div>
      <div id="richtextArea" style='display: none;'><textarea
        name="richtextEditor" style="width: 300px; height: 400px"
        cols="60" rows="20" id="richtextEditor"></textarea></div>
      </td>
    </tr>
    <tr>
      <td>${Context.getMessage("label.page.push")}</td>
      <td><input type="radio" name="pushToMenu" value="true"
        checked="true" />${Context.getMessage("label.page.push.yes")} <input
        type="radio" name="pushToMenu" value="false" />${Context.getMessage("label.page.push.no")}
      </td>
    </tr>
    <tr>
      <td colspan="2">
        <input type="submit" value="${Context.getMessage("action_save")}" />&nbsp;
        <input type="button" value="${Context.getMessage("action_cancel")}" onclick="document.pageCreate.action='${This.path}/view'; document.pageCreate.submit();" />
      </td>
    </tr>
  </tbody>
</table>
</form>

<script>
function launchEditor() {
  $('#wiki_editor').markItUp(myWikiSettings);
  document.getElementById('wikitext').checked = true;
}

$('#richtextEditor').ready(
function() {
document.getElementById('wikitext').checked = true;

document.tmceCreate = new tinymce.Editor(
  'richtextEditor',
  {
    theme :"advanced",
    plugins :"safari,spellchecker,pagebreak,style,layer,table,save,advhr,advimage,advlink,emotions,iespell,inlinepopups,insertdatetime,preview,media",
    
    // Theme options
    theme_advanced_buttons1 :"save,newdocument,|,bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,|,styleselect,formatselect,fontselect,fontsizeselect",
    theme_advanced_buttons2 :"cut,copy,paste,pastetext,pasteword,|,search,replace,|,bullist,numlist,|,outdent,indent,blockquote,|,undo,redo,|,link,unlink,anchor,image,cleanup,help,code,|,insertdate,inserttime,preview,|,forecolor,backcolor",
    theme_advanced_buttons3 :"tablecontrols,|,hr,removeformat,visualaid,|,sub,sup,|,charmap,emotions,iespell,media,advhr,|,print,|,ltr,rtl,|,fullscreen",
    theme_advanced_buttons4 :"insertlayer,moveforward,movebackward,absolute,|,styleprops,spellchecker,|,cite,abbr,acronym,del,ins,attribs,|,visualchars,nonbreaking,template,blockquote,pagebreak,|,insertfile,insertimage",
    theme_advanced_toolbar_location :"top",
    theme_advanced_toolbar_align :"left",
    theme_advanced_statusbar_location :"bottom",
    theme_advanced_resizing :true
  });
  	
  
  document.tmceCreate.render();
});

$('#wiki_editor').ready( function() {
  setTimeout(launchEditor, 10);
});
</script>

</#macro>

