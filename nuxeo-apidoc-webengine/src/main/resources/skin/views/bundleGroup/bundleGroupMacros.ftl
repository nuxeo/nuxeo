<#include "/views/bundle/bundleMacros.ftl">
<#macro viewBundleGroup bundleGroupWO >

  <#assign nestedLevel=nestedLevel+1/>
  <#assign bundleGroupItem=bundleGroupWO.getNxArtifact()/>
  <#assign bundleGroupDocs=bundleGroupWO.getAssociatedDocuments()/>
  <#assign bundleGroupDesc=bundleGroupDocs.getDescription(Context.getCoreSession())/>
  <#assign bundles=bundleGroupWO.getBundles()/>
  <#assign subGroups=bundleGroupWO.getSubGroups()/>

  <#assign quickEditId=bundleGroupDesc.getEditId()/>

<script>

var editorDisplayed=false;

function quickEditSave(id) {

 var targetUrl = document.location.href
 if (targetUrl.substr(-1)!="/") {
  targetUrl +="/";
 }
 targetUrl += 'quickEdit/' + id;

 var content = $('#liveQuickEditor').val();
 var title=$('#liveQuickEditorTitle').val();

$.post(targetUrl, {id: id, title: title, content: content} ,function(data) {
  document.location.href=document.location.href;
});

}

function quickEditShow(id) {

 var targetUrl = document.location.href
 if (targetUrl.substr(-1)!="/") {
  targetUrl +="/";
 }
 targetUrl += 'quickEdit/' + id;


var isPlaceHolder=false;

if (id.match("^placeholder_")=="placeholder_") {
 isPlaceHolder=true;
}
if (editorDisplayed==true) {
 return;
}
editorDisplayed=true;

var titleItem = $("span").filter(function() { return this.id==(id+'_doctitle');})[0]
var textItem = $("div").filter(function() { return this.id==(id+'_doccontent');})[0]

var editTitle="<input class='quickEdit' id='liveQuickEditorTitle' type='text' size='50' value='" + $(titleItem).html() + "' >";
$(editTitle).insertBefore($(titleItem));
$(titleItem).css("display","none");

var textContent="Loading...";
if (isPlaceHolder) {
  textContent="";
}

var editContent="<textarea id='liveQuickEditor' class='quickEdit' cols='120' rows='20' >" + textContent + "</textarea>";
$(editContent).insertBefore($(textItem));
$(textItem).css("display","none");

var saveButton="<img src='${skinPath}/images/save.gif' alt='Save' onclick='return quickEditSave(\"" + id + "\")' />";

var editBtn = $("img").filter(function() { return this.id==(id+'_button');})[0]
$(saveButton).insertAfter($(editBtn));
$(editBtn).css("display","none");

if (isPlaceHolder) {
  return;
}

$.get(targetUrl, function(data) {
  $('#liveQuickEditor').attr("enabled","false");
  $('#liveQuickEditor').html(data);
});



return true;
}



</script>

  <div id="BundleGroup.${bundleGroupItem.id}_frame" class="blocFrame" style="margin-left:${nestedLevel*6}px">
  <div class="blocTitle bTitle${nestedLevel}" id="${bundleGroupItem.id}"> BundleGroup <span id="${quickEditId}_doctitle" >${bundleGroupDesc.title}</span>

<#if Root.isEditor()>
  <!--<A href="${Root.path}/${distId}/viewBundleGroup/${bundleGroupItem.id}/doc">-->
  <img src="${skinPath}/images/edit.png" alt="Edit" id='${quickEditId}_button'  onclick="return quickEditShow('${quickEditId}')"/>

</#if>

  </div>

  <div class="foldablePanel">
  <p><@docContent docItem=bundleGroupDesc /></p>

  <#if (subGroups?size>0)>
  ${bundleGroupDesc.title} contains ${subGroups?size} sub BundleGroups.

  <table class="linkTable">
  <#list subGroups as subGroup>
  <tr>
  <td>
  ${subGroup.associatedDocuments.getDescription(Context.getCoreSession()).title}
  </td>
  <td>
  <A href="#BundleGroup.${subGroup.nxArtifact.id}">${subGroup.nxArtifact.id}</A>
  </td>
  </tr>
  </#list>
  </table>

  <#list subGroups as subGroup>
   <@viewBundleGroup bundleGroupWO=subGroup />
  </#list>
  </#if>

  <span class="builtindoc">
  ${bundleGroupDesc.title} is composed of ${bundles?size} bundles.

  <table class="linkTable">
  <#list bundles as bundle>
  <tr>
  <td>
  ${bundle.associatedDocuments.getDescription(Context.getCoreSession()).title}
  </td>
  <td>
  <A href="#Bundle.${bundle.nxArtifact.id}">${bundle.nxArtifact.id}</A>
  </td>
  </tr>
  </#list>
  </table>

  <#list bundles as bundle>
   <@viewBundle bundleWO=bundle />
  </#list>
  </span>
  <@viewAdditionalDoc docsByCat=bundleGroupDocs.getDocumentationItems(Context.getCoreSession())/>
  </div>
  </div>

  <#assign nestedLevel=nestedLevel-1/>
</#macro>