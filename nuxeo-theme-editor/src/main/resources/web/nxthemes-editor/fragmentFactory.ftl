<#assign fragments = script("getFragments.groovy") />

<div class="nxthemesCollapsible nxthemesToolbox" id="nxthemesFragmentFactory">

<div class="title">
<img class="close" onclick="javascript:NXThemesEditor.backToCanvas()"
     src="/nuxeo/site/files/nxthemes-editor/img/close-button.png" width="14" height="14" alt="" />
Fragments
</div>
<ul>
  <#list fragments as fragment>
    <li class="fragment">${fragment.fragmentType.typeName}</li>
    <ul class="views" style="display:none">
      <#list fragment.views as v>
        <li class="nxthemesFragmentFactory" title="Drag this widget to the canvas"
          typename="${fragment.fragmentType.typeName}/${v.viewName}"><img src="${v.icon}" width="16" height="16" /> ${v.viewName}</li>
      </#list>
    </ul>
  </#list>
</ul>

</div>
