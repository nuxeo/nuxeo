<select onchange="location.href=this.value">
  <#if operation?has_content>
    <option value="${This.path}?">View All Operations</option>
  <#else>
    <option value="${This.path}?" selected disabled>Select Operation</option>
  </#if>
  <#list categories?keys as cat>
    <optgroup label="${cat?xml}">
      <#list categories["${cat}"] as item>
        <#if operation?has_content && operation.id = item.id>
            <option value="${This.path}?id=${item.id}" selected>${item.label} - (id:${item.id}<#if item.aliases> - alias:[<#list item.aliases as alias> ${alias} </#list>]</#if>)</option>
        <#else>
            <option value="${This.path}?id=${item.id}">${item.label} - (id:${item.id}<#if item.aliases> - alias:[<#list item.aliases as alias> ${alias} </#list>]</#if>)</option>
        </#if>
      </#list>
    </optgroup>
  </#list>
</select>