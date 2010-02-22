<!-- markitup -->
<script src="${skinPath}/script/markitup/jquery.markitup.pack.js"></script>
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/skins/markitup/style.css" />
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/sets/wiki/style.css" />
<!-- end markitup -->

<!-- tinyMCE -->
<script type="text/javascript" src="${skinPath}/script/tiny_mce/tiny_mce.js"></script>
<script type="text/javascript" src="${skinPath}/script/tiny_mce/langs/en.js"></script>
<script type="text/javascript" src="${skinPath}/script/tiny_mce/themes/simple/editor_template.js"></script>
<!-- end tinyMCE -->

<form method="POST"
<#if mode=='create'>
  action="${This.path}/createDocumentation"
</#if>
<#if mode=='edit'>
  action="${This.path}/updateDocumentation"
</#if>

enctype="application/x-www-form-urlencoded">

<table width="90%">
<tr><td>
  <table>
  <tr>
    <td> Type : </td>
    <td>
    <#if mode=='create'>
      <select name="type">
      <#assign categories=This.categories/>
      <#list categories?keys as category>
      <option value="${category}"
      <#if category==docItem.type>
        selected
      </#if>
      >${categories[category]}</option>
      </#list>
      </select>
    </#if>
    <#if mode=='edit'>
     ${docItem.typeLabel}
    </#if>
    </td>
  </tr>
  <tr>
    <td> Title : </td>
    <td> <input type="text" name="title" value="${docItem.title}" size="80"/> </td>
  </tr>
  <tr>
    <td> Content Type : </td>
    <td> <select name="renderingType" onchange="changeEditor(this.value)">
      <option value="wiki"
      <#if docItem.renderingType=='wiki'>
       selected
      </#if>
      > Wiki </option>

      <option value="html"
      <#if docItem.renderingType=='html'>
       selected
      </#if>
      > Html </option>
    </select>
  </tr>
  <tr>
    <td> Content : </td>
    <td><div id="contentEditorContainer">
     <textarea id="contentEditor" name="content" cols="80" rows="20">${docItem.content}</textarea>
     </div>
    </td>
  </tr>
  </table>
</td>
<td>
  <table>
  <tr>
      <td> Approved by Nuxeo : </td>
      <td> <input type="checkbox" name="approved"
      <#if docItem.approved>
       checked
      </#if>
      /> </td>
  </tr>
  <tr>
    <td> Applicable versions : </td>
    <td> <select size="3" name="versions" multiple="multiple">
    <#list versions as version>
    <option value="${version}"

    <#if docItem.applicableVersion?seq_contains(version)>
      selected
    </#if>

    >
    ${version}</option>
    </#list>
    </select>
   </td>
  </tr>
  </table>
</td>
</tr>
</table>

<input type="hidden" name="id" value="${docItem.id}"/>
<input type="hidden" name="uuid" value="${docItem.getUUID()}"/>
<input type="hidden" name="targetType" value="${docItem.targetType}"/>
<input type="hidden" name="target" value="${docItem.target}"/>

<#if mode=='create'>
  <input type="submit" value="Create"/>
</#if>
<#if mode=='edit'>
    <input type="submit" value="Update"/>
</#if>

<input type="button" value="Cancel" onclick="history.back()"/>

</form>


<script>
function launchWikiEditor() {
  wikiSettings = {
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
        {name:'Image', key:"T", replaceWith:'[[Image:[![Url:!:http://]!]|[![name]!]]]'},
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

  $('#contentEditor').markItUp(wikiSettings);
}

function launchHtmlEditor() {
  document.tmceEdit = new tinymce.Editor('contentEditor',{
  mode : "textareas",
  theme : "advanced",
  editor_selector : "mceAdvanced"
    });

  document.tmceEdit.render();
}

function changeEditor(type) {
    $('#contentEditor').appendTo('#contentEditorContainer');
    $('#contentEditor').siblings().remove();
    $('#contentEditor').removeClass().removeAttr('style');

    if (type=="html") {
        launchHtmlEditor()
    }
    else {
        launchWikiEditor()
    }
}
$('#contentEditor').ready(function() {
<#if docItem.renderingType=='html'>
  setTimeout(launchHtmlEditor, 10);
</#if>

<#if docItem.renderingType=='wiki' || docItem.renderingType==null>
  setTimeout(launchWikiEditor, 10);
</#if>
});
</script>
