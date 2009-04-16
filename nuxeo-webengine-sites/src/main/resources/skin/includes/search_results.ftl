<#macro searchResults>

<div class="searchResultpageBlock">
  <h4>${Context.getMessage("title.search.results")}</h4>
  <#list results as p>
  <div class="searchResultBlock">
  <div class="searchName">
    <a href="${p.path}"> ${p.name}</a>
  </div>
  <div class="searchDetail">
    <p><span>${p.author}</span>&nbsp;|&nbsp;<span>${p.description}</span></p>
  </div>
  <div class="searchResultDates">
    <span>${Context.getMessage("label.webpage.creation.date")}: ${p.created}</span>
    <span class="modificationDate">${Context.getMessage("label.webpage.modification.date")}: ${p.modified}</span>
  </div>
  </div>
  </#list>
</div>

</#macro>