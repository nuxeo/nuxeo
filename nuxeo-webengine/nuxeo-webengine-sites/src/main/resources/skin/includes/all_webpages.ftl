<#macro allWebpages>

<div class="allWebPagesBlock"> 
  
  <#if webPages?size != 0>
    <h4>${Context.getMessage("title.all.webpages")}</h4>
    <div class="allWebPagesResume">    
      <#list webPages as webPage>  
        <div class="documentInfo"> 
          <a href="${This.path}/${webPage.path}"> ${webPage.name} </a>
        </div>
        <div style="clear:both;"></div>
      </#list>
    </div>
  </#if>
</div>

</#macro>
        
        
        
        