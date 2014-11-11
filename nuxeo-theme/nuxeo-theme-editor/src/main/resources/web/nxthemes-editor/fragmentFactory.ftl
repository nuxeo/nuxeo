<#assign fragments = script("getFragments.groovy") />

<div class="nxthemesCollapsible nxthemesToolbox" id="nxthemesFragmentFactory">

<div class="title">
<img class="close" onclick="javascript:NXThemesEditor.backToCanvas()"
     src="/nuxeo/site/files/nxthemes-editor/img/close-button.png" width="14" height="14" alt="" />
Fragments
</div>
<ul>
  <#list fragments as fragment>
    <li class="fragment">${fragment.getFragmentType().getTypeName()}</li>
    <ul class="views" style="display:none">
      <#list fragment.getViews() as v>
        <li class="nxthemesFragmentFactory" title="Drag this widget to the canvas"
          typename="${fragment.getFragmentType().getTypeName()}/${v.getViewName()}"><img src="${v.getIcon()}" width="16" height="16" /> ${v.getViewName()}</li>
      </#list>
    </ul>
  </#list>
</ul>

</div>
