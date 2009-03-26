<#macro searchResults>

<div class="lastPublishedPagesBlock">
  <h4>${Context.getMessage("title.search.results")}</h4>
  <#list results as p>
  <div class="pagePublishedResume">
    <div class="documentInfo"> 
    <a href="${p.path}"> ${p.name}</a>
        <p><span>${p.author}</span>&nbsp;|&nbsp;<span>${p.description}</span></p>
  </div>
  </div>
  <div class="dayMonth">
   <span>${Context.getMessage("label.webpage.creation.date")}</span><span>: ${p.created}</span>
   <br/>
   <span>${Context.getMessage("label.webpage.modification.date")}</span><span>: ${p.modified}</span>
  </div>
   <div style="clear:both;"></div>
  </#list>
</div>

</#macro>