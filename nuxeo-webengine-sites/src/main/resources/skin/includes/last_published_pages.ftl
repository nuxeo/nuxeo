<#macro lastPublished>

<div class="lastPublishedPagesBlock">
  <h4>${Context.getMessage("title.last.published.pages")}</h4>
  <#list pages as p>
  <div class="pagePublishedResume">
  <div class="dayMonth"><span>${p.day}<br/></span>${p.month}</div>
  <div class="documentInfo"> 
    <a href="${p.path}"> ${p.name}</a>
        <p><span>${p.author}</span>&nbsp;|&nbsp;<span>${p.numberComments} ${Context.getMessage("last.published.pages.comments")}</span></p>
   </div>
   <div style="clear:both;"></div>
   </div>
  </#list>
</div>

</#macro>