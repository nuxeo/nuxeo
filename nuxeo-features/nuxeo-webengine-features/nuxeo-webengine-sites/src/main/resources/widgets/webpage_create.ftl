<!-- markitup -->
<script src="${skinPath}/script/markitup/jquery.markitup.pack.js"></script>
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/skins/markitup/style.css" />
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/sets/wiki/style.css" />
<!-- end markitup -->

<!-- tinyMCE -->
<script type="text/javascript" src="${skinPath}/script/tiny_mce/tiny_mce.js"></script>
<script type="text/javascript" src="${skinPath}/script/tiny_mce/tiny_mce_init.js"></script>
<!-- end tinyMCE -->

<script type="text/javascript">
	tinymce.baseURL = "${skinPath}/script/tiny_mce";
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
<input type="hidden" name="pageName" value="${Context.request.getAttribute('pageName')}" />
<table class="createWebPage">
  <tbody>
  <tr>
    <td>${Context.getMessage("label.page.title")}</td>
  </tr>
  <tr>
    <td><input type="text" id="titleInput" name="title" value="${Context.request.getAttribute('pageName')}" /></td>
   </tr>
   <tr>
      <td>${Context.getMessage("label.page.description")}</td>
    </tr>
    <tr>
      <td><textarea name="description"></textarea></td>
    </tr>
    <tr>
      <td>${Context.getMessage("label.page.format")}</td>
    </tr>
    <tr>
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
        name="richtextEditor"  class="mceEditor" style="width: 300px; height: 400px"
        cols="60" rows="20" id="richtextEditor"></textarea></div>
      </td>
    </tr>
    <tr>
      <td>${Context.getMessage("label.page.push")}</td>
    </tr>

    <tr>
      <td><input type="radio" name="pushToMenu" value="true"
        checked="true" />${Context.getMessage("label.page.push.yes")} <input
        type="radio" name="pushToMenu" value="false" />${Context.getMessage("label.page.push.no")}
      </td>
    </tr>
    <tr>
      <td colspan="2">
        <input type="submit" class="button" value="${Context.getMessage("action_save")}" />&nbsp;
        <input type="button" class="button" value="${Context.getMessage("action_cancel")}" onclick="document.pageCreate.action='${This.path}/@perspective/view'; document.pageCreate.submit();" />
      </td>
    </tr>
  </tbody>
</table>
</form>

<script>
function launchEditor() {
  mySitesWikiSettings = {
    nameSpace:          "wiki", // Useful to prevent multi-instances CSS conflict
    previewParserPath:   "${This.path}/@views/preview",
    previewParserVar: 'wiki_editor',
    previewAutorefresh: true,
    previewInWindow: 'width=500, height=700, resizable=yes, scrollbars=yes',
    onShiftEnter:       {keepDefault:false, replaceWith:'\n\n'},
    markupSet:  [
      {name:'Heading 1', key:'1', openWith:'== ', closeWith:' ==', placeHolder:'Your title here...' },
      {name:'Heading 2', key:'2', openWith:'=== ', closeWith:' ===', placeHolder:'Your title here...' },
      {name:'Heading 3', key:'3', openWith:'==== ', closeWith:' ====', placeHolder:'Your title here...' },
      {name:'Heading 4', key:'4', openWith:'===== ', closeWith:' =====', placeHolder:'Your title here...' },
      {name:'Heading 5', key:'5', openWith:'====== ', closeWith:' ======', placeHolder:'Your title here...' },
      {separator:'---------------' },
      {name:'Bold', key:'B', openWith:"**", closeWith:"**"},
      {name:'Italic', key:'I', openWith:"__", closeWith:"__"},
      //{name:'Stroke through', key:'S', openWith:'<s>', closeWith:'</s>'},
      {separator:'---------------' },
      {name:'Bulleted list', openWith:'(!(- |!|-)!)'},
      {name:'Numeric list', openWith:'(!(+ |!|+)!)'},
      {separator:'---------------' },
      {name:'Image', key:"T", replaceWith:'[image:[![Url:!:http://]!] [![name]!]]'},
      {name:'Link', key:"L", openWith:"[[![Link]!] ", closeWith:']', placeHolder:'Your text to link here...' },
      {name:'Url', openWith:"[[![Url:!:http://]!] ", closeWith:']', placeHolder:'Your text to link here...' },
      {separator:'---------------' },
      {name:'Quotes', openWith:'(!(> |!|>)!)'},
      {name:'Inline Code', openWith:'$$', closeWith:'$$'},
      {name:'Code', openWith:'{{{', closeWith:'}}}'},
      {separator:'---------------' },
      {name:'Preview', key: 'P', call:'preview', className:'preview'}
    ]
  };

  $('#wiki_editor').markItUp(mySitesWikiSettings);
  document.getElementById('wikitext').checked = true;
}

$('#richtextEditor').ready(
function() {
document.getElementById('wikitext').checked = true;
});

$('#wiki_editor').ready( function() {
  setTimeout(launchEditor, 10);
});
</script>