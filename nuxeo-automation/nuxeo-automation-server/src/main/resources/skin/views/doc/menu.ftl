<nav role="navigation">
  <ul>
    <#list categories?keys as cat>
    <li class="category">${cat?xml}</li>
    <ol class="category_content">
      <#list categories["${cat}"] as item>
        <#if operation?has_content && operation.id = item.id>
          <li class="item selected"><a href="?id=${item.id}">${item.label}</a></li>
        <#else>
          <li class="item"><a href="?id=${item.id}">${item.label}</a></li>
        </#if>
      </#list>
    </ol>
    </#list>
  </ul>
</nav>
