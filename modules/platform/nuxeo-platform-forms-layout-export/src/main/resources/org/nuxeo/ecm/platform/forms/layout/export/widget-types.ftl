<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
    <title>Nuxeo Layout Documentation</title>
  </head>
  <style>
  .tree {
    border-left: 1px solid black;
    border-right: 1px solid black;
    border-bottom: 1px solid black;
  }
  .category {
    border-top: 1px solid black;
    border-bottom: 1px solid black;
    padding: 2px;
    background-color: #cafeca
  }
  .export_link {
    border-top: 1px solid black;
    border-bottom: 1px solid black;
    padding: 2px;
  }
  .category_content {
    padding-top: 10px;
    padding-bottom: 10px;
  }
  .item {
    margin-left: 20px;
  }
  .content {
    margin: 10px 10px;
  }

  .params table {
    border: 1px solid black;
    border-collapse: collapse;
  }

  .params th {
    background-color: #cecece;
  }

  .params td,th {
    border: 1px solid black;
    padding: 6px;
  }


h1 {
/* H1 */
    font: normal small-caps bold 240% "Century Gothic", "Trebuchet MS", Verdana, sans-serif;
    color: #000;
    border-bottom: 1px solid #333;
    margin: 0;
    margin-bottom: 10px;
}

h2 {
/* H2 */
    background-color: #f1f2f3;
    font: normal normal bold 150% "Century Gothic", "Trebuchet MS", Verdana, sans-serif;
    color: #111;
    margin: 0px;
    margin: 15px 0px 10px 0px;

}

h3 {
/* H3 */
    background-color: #fff;
    font: normal normal bold 133% "Century Gothic", "Trebuchet MS", Verdana, sans-serif;
    margin: 10px 0 5px 0;
}

h4 {
    margin: 5px 0 2px 0;
    background-color: #fff;
}

pre {
    background-color: #F1F7FF;
    border: 1px dotted #555555;
    margin: 10px;
    padding: 7px;
    white-space: pre;
    font-size: 0.9em;
}
</style>

<body>

  <table width="100%" class="main_table">
    <tr>
      <td colspan="2" align="right">
        <a href="${baseURL}../">Top</a> - <a href="${baseURL}?widgetTypeCategory=${widgetTypeCategory}">Index</a> - <a href="${baseURL}wiki/?widgetTypeCategory=${widgetTypeCategory}">Wiki export</a>
      </td>
    </tr>
    <tr valign="top">
      <td width="30%">
        <!-- menu -->
        <div class="tree">
          <#list categories?keys as cat>
            <div class="category">${cat}</div>
            <div class="export_link">
              <a href="${baseURL}widgetTypes/${cat}?all=true&widgetTypeCategory=${widgetTypeCategory}">JSON definitions</a>
              <#list nuxeoVersions as nxv>
                <a href="${baseURL}widgetTypes/${cat}?version=${nxv}&all=true&widgetTypeCategory=${widgetTypeCategory}">${nxv}</a>
              </#list>
            </div>
            <div class="category_content">
              <#list categories["${cat}"] as widgetType>
                <div class="item">
                  <a href="${baseURL}?widgetType=${widgetType.name}&widgetTypeCategory=${widgetTypeCategory}">
                    ${This.getWidgetTypeLabel(widgetType)}
                  </a>
                </div>
              </#list>
            </div>
          </#list>
          <div class="export_link">
            <a href="${baseURL}widgetTypes/${studioCategories}?all=true&widgetTypeCategory=${widgetTypeCategory}">All Studio JSON definitions</a>
              <#list nuxeoVersions as nxv>
                <a href="${baseURL}widgetTypes/${studioCategories}?version=${nxv}&all=true&widgetTypeCategory=${widgetTypeCategory}">${nxv}</a>
              </#list>
          </div>
          <div class="export_link">
            <a href="${baseURL}widgetTypes?all=true&widgetTypeCategory=${widgetTypeCategory}">All JSON definitions</a>
              <#list nuxeoVersions as nxv>
                <a href="${baseURL}widgetTypes?version=${nxv}&all=true&widgetTypeCategory=${widgetTypeCategory}">${nxv}</a>
              </#list>
          </div>
        </div>
      </td>
      <td>
        <div class="content">
          <#if widgetType?has_content>
            <h1>${This.getWidgetTypeLabel(widgetType)}</h1>
            <div class="description">
              ${This.getWidgetTypeDescription(widgetType)}
            </div>
            <h2>General Information</h2>
            <div class="info">
              <div>
                <b>Categories:</b> ${This.getWidgetTypeCategoriesAsString(widgetType)}
              </div>
              <div>
                <b>Widget type name:</b> ${widgetType.name}
              </div>
            </div>
            <h2>Links</h2>
            <div>
              <a href="${baseURL}widgetType/${widgetType.name}?widgetTypeCategory=${widgetTypeCategory}">JSON definition</a>
            </div>
          <#else>
            <h1>Index</h1>
            <#list widgetTypes as widgetType>
              <div class="index_item">
                <a href="${baseURL}?widgetType=${widgetType.name}&widgetTypeCategory=${widgetTypeCategory}">${This.getWidgetTypeLabel(widgetType)}</a>
              </div>
            </#list>
          </#if>
        </div>
      </td>
    </tr>
  </table>

</body>

</html>