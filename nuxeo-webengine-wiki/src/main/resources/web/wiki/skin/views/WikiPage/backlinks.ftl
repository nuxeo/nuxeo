<#assign links = script("backlinks/RelationHelper.groovy") />

<#list links as link>
  <a href=${link.href}>${link.title}</a><br>
</#list>

