<?xml version="1.0"?>
<component name="${contribution.component.id}.override">

  <require>${contribution.component.id}</require>

<!--
  ${ep.documentation}
-->

  <extension target="${contribution.extensionPoint?split("--")[0]}"
    point="${contribution.extensionPoint?split("--")[1]}">

   <#list contribution.contributionItems as contributionItem>
     <#if selectedContribs?seq_contains(contributionItem.id)>
    ${contributionItem.rawXml}
     </#if>
   </#list>

  </extension>

</component>
