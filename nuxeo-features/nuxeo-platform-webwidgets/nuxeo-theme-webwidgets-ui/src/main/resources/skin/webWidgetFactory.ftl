 
<div class="nxthemesWebWidgetFactoryBox">

  <div class="header">
    <a style="float: right" href="javascript: NXThemesWebWidgets.exit()"><img src="${skinPath}/img/close.png" width="16" height="16" /></a>
     <span>Web widgets</span>
    <form action="javascript:void(0)" submit="return false" style="margin: 0; padding: 8px 0">

      <select id="webWidgetCategory" style="width: 100%; border: 1px solid #666"
        onchange="NXThemesWebWidgets.setWidgetCategory(this)">
        <option value="">All categories</option>
        <#list widget_categories as category>
          <#if category = selected_category>
            <option value="${category}" selected="selected">&raquo; ${category}</option>
          <#else>
            <option value="${category}">&raquo; ${category}</option>
          </#if>
        </#list>
      </select>
    </form>
  </div>

  <ul class="body">
    <#list widget_types as type>
      <li class="nxthemesWebWidgetFactory"
           title="${type.description}"
           typename="${type.typeName}">
           <img src="${basePath}/nxthemes-webwidgets/render_widget_icon?name=${type.typeName}" width="16" height="16" alt="" title="Add widget" />
           ${type.typeName}
      </li>
    </#list>
  </ul>

</div>
