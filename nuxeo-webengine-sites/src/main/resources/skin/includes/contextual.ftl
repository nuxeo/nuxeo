<#macro contextual>

<div class="contextualLinks">
  <h4>${Context.getMessage("title.contextual.links.pages")}</h4>
  <#list contextualLinks as cl>
  <div class="contextualLinkBlock">
    <div class="contextualLink"><a href="${cl.link}" target="_blank"> ${cl.title} &nbsp; </a></div>
    <div class="contextualLinkText">${cl.description}</div>
  </div>
  </#list>
</div>
</#macro>