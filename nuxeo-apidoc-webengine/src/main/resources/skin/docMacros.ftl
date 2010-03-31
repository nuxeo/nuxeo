<#macro docContent docItem>
  <div class="docContent">
  <#if docItem.renderingType=='html'>
      ${docItem.content}
  </#if>
  <#if docItem.renderingType=='wiki'>
      <@wiki>${docItem.content}</@wiki>
  </#if>
    <#if docItem.applicableVersion??>
      <#if ((docItem.applicableVersion?size)>0) >
      <div class="docVersionDisplay">
        <ul>
        <#list docItem.applicableVersion as version>
          <li>${version}</li>
        </#list>
        </ul>
      </div>
      </#if>
    </#if>
  </div>
</#macro>
