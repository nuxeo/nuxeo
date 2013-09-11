{toc:maxLevel=3}

<#list categories?keys as cat>
h1. ${cat}
  <#list categories["${cat}"] as widgetType>

{multi-excerpt:name=${widgetType.name}}

h2. ${This.getWidgetTypeLabel(widgetType)}

{html}${This.getWidgetTypeDescription(widgetType)}{html}

h5. General Information

*Category:* ${This.getWidgetTypeCategoriesAsString(widgetType)}
*Widget type name:* ${widgetType.name}

{multi-excerpt}

  </#list>
</#list>
