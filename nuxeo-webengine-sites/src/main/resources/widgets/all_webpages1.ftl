<#macro lastPublished>

<div class="allWebPagesBlock">
  <h4>${Context.getMessage("title.all.web.pages")}</h4>
  
  <div class="allWebPagesResume">
  <#assign webPages = script("getWebPages.groovy") />
  <#list webPages as webPage>  
    <div class="documentInfo"> 
      <a href="${This.path}/${webPage.path}"> ${webPage.name}</a>
     </div>
     <div style="clear:both;"></div>
    </div>
  </#list>

</div>

</#macro>
        
        
        
        
        
