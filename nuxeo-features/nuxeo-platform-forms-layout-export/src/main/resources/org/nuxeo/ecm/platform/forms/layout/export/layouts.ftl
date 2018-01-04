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
        <a href="${baseURL}../">Top</a> - <a href="${baseURL}?">Index</a>
      </td>
    </tr>
    <tr valign="top">
      <td width="30%">
        <!-- menu -->
        <div class="tree">
          <div class="category">Registered Layouts</div>
          <div class="category_content">${cat}</div>
            <#list layoutNames as layoutName>
              <div class="item">
                <a href="${baseURL}?layoutName=${layoutName}">
                  ${layoutName}
                </a>
              </div>
            </#list>
          </div>
      </td>
      <td>
        <div class="content">
          <#if layoutDefinition?has_content>
            <h1>${layoutDefinition.name}</h1>
            <table border="1">
            <#list layoutDefinition.getRows() as rowDef>
              <tr>
                 <#list rowDef.getWidgetReferences() as widgetRef>
                 <td>
                   <#if layoutDefinition.getWidgetDefinition(widgetRef.getName())==null>
                    ${widgetRef.getName()}
                   <#else>
                    <A href="${baseURL}../widget-types/?widgetType=${layoutDefinition.getWidgetDefinition(widgetRef.getName()).getType()}">${widgetRef.getName()}</A>
                   </#if>
                 </td>
                 </#list>
                 <#if rowDef.getWidgetReferences()?size < layoutDefinition.getColumns()>
                 <td colspan="${layoutDefinition.getColumns() - rowDef.getWidgetReferences()?size}"> &nbsp; </td>
                 </#if>
              </tr>
            </#list>
            </table>
            <h2>Links</h2>
            <div>
              <a href="${baseURL}json?layoutName=${layoutDefinition.name}">JSON definition</a>
            </div>
          </#if>
        </div>
      </td>
    </tr>
  </table>

</body>

</html>