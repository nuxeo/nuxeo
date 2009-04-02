<#macro webPageCreate>
<!-- markit up -->
<script src="${skinPath}/script/markitup/jquery.markitup.pack.js"></script>
<script src="${skinPath}/script/markitup/sets/wiki/set.js"></script>
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/skins/markitup/style.css" />
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/sets/wiki/style.css" />
<!-- end markitup -->

<!-- tinyMCE -->
<script type="text/javascript" src="${skinPath}/script/tiny_mce/tiny_mce.js"></script>
<!-- end tinyMCE -->

<base href="${This.urlPath}">
<form method="POST" action="${This.path}/createWebPage" accept-charset="utf-8">
  <table class="createWebPage">
    <tbody>
      <tr>
        <td width="30%"></td>
        <td width="70%"></td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.page.title")}</td>
        <td><input type="text" name="title" value=""/></td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.page.description")}</td>
        <td><textarea name="description"></textarea></td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.page.format")}</td>
        <td>
          <input type="radio" name="format" value="wikitext" checked="true" />${Context.getMessage("label.page.format.wikitext")}
          <input type="radio" name="format" value="richtext" />${Context.getMessage("label.page.format.richtext")}
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <div id="wikitextArea">
            <textarea name="wikitextEditor" cols="60" rows="20" id="wiki_editor" class="entryEdit"></textarea>
          </div> 
          <div id="richtextArea">
            <textarea name="richtextEditor" cols="60" rows="20" id="rich_editor" class="entryEdit"></textarea> 
          </div> 
        </td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.page.push")}</td>
        <td>
          <input type="radio" name="pushToMenu" value="true" checked="true" />${Context.getMessage("label.page.push.yes")}
          <input type="radio" name="pushToMenu" value="false" />${Context.getMessage("label.page.push.no")}
        </td>
      </tr>
      <tr>
        <td colspan="2"><input type="submit" value="Save"/></td>
      </tr>
    </tbody>
  </table>  
</form>

<script>
function launchEditor() {
  $('#wiki_editor').markItUp(myWikiSettings);
}

$('#wiki_editor').ready(function() {
  setTimeout(launchEditor, 10);
});
</script>

<script>
tinyMCE.init({
  // General options
  mode : "exact",
  elements : "rich_editor",
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
  theme_advanced_resizing : true,

});
</script>

</#macro>

