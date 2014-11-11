<div id="nxthemesPageSelector" class="nxthemesPageTabs">
  <div class="themeName">${current_theme_name}/ </div>
  <ul>
    <#list pages as page>
      <li class='${page.className}'><span><a class="switcher" href="javascript:void(0)"
        name="${page.link}">${page.name}</a></span></li>
    </#list>
    <#if pages>
    <li><span><a title="Add a new page"
        style="font-weight: bold" href="javascript:void(0)" onclick="javascript:NXThemesEditor.addPage('${current_theme_name?js_string}')">&nbsp;+&nbsp;</a></span></li>
    </#if>
  </ul>
  <#if !pages><div style="padding: 3px 20px; color: #ccc; font-weight: bold">... no pages found</div></#if>
</div>
