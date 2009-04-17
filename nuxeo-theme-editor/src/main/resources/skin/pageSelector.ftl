<div id="nxthemesPageSelectorArea" style="width: 100%">

<div class="nxthemesTabs nxthemesPageTabs">
  <ul>
    <#list pages as page>
      <li class='${page.className}'><a class="switcher" href="javascript:void(0)"
        name="${page.link}">${page.name}</a></li>
    </#list>
    <li><a style="font-weight: bold" href="javascript:void(0)" onclick="javascript:NXThemesEditor.addPage('${current_theme_name}')">add page</a></li>
  </ul>

<div style="clear: both"></div>
</div>

</div>
