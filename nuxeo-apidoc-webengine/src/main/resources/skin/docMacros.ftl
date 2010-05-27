<#macro docContent docItem>
  <div class="docContent" id="${docItem.editId}_doccontent">
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

    <#assign attachments=docItem.attachments>
    <#if attachments??>
      <#assign attachmentKeys=attachments?keys>
      <#list attachmentKeys as attachmentName>
       <div class="attachmentTitle">${attachmentName}</div>
       <div class="attachmentContent">
       <pre><code>${attachments[attachmentName]?html}</code></pre>
       </div>
      </#list>
    </#if>

  </div>
</#macro>

<#macro inlineEdit cid contentDoc>
  <span id="${cid}_panel">
    <#if Root.isEditor()><!--
     <A href="javascript:doEditInline('${cid}_panel','${cid}')">
     <img src="${skinPath}/images/edit.png" alt="Edit"/>
     </A>-->
    </#if>
    <#if contentDoc!=null>
    (${contentDoc.title}) <br/>
    <div class="description">
       <@docContent contentDoc/> <br/>
    </div>
    </#if>
    <#if contentDoc==null>
      No associated documentation <br/>
    </#if>
  </span>

</#macro>

<#macro viewAdditionalDoc docsByCat>
  <#assign categories=docsByCat?keys/>
  <#if ((categories?size)>1)>
    <div class="additionalDocs"> Additional documentation <br/>
    <table>
    <tr>
    <#list categories as category>

     <#if !(category=="Description")>
       <td class="catHeader">
       ${category}
       </td>
       <td>
       <ul>
       <#list docsByCat[category] as docItem>
          <li><A href="javascript:showAddDoc('${docItem.id}')">${docItem.title}</A> </li>
       </#list>
       </ul>
       </td>
       <td>&nbsp;
       </td>
     </#if>
    </#list>
    </tr>
    </table>

    <#list categories as category>
     <#list docsByCat[category] as docItem>
        <div class="additionalDocPanel hiddenDocPanel" id="${docItem.id}">
        <A name="${docItem.id}"> ${docItem.title} </A>
          <@docContent docItem/>
        </div>
     </#list>
    </#list>

    </div>
  </#if>
</#macro>

<#macro filterForm resultSize artifactType>
  <#if searchFilter??>
    <h1> All ${artifactType}s with filter '${searchFilter}' (${resultSize}) </h1>
  </#if>
  <#if !searchFilter>
    <h1> All ${artifactType}s (${resultSize})</h1>
  </#if>

  <p>
  Here is the list of the ${artifactType}s present in the selected distribution (${distId}).
  </p>

  <#if !Root.currentDistribution.live>
    <span style="float:right">
    <form method="GET" action="${Root.path}/${distId}/filter${artifactType}s" >
      <input type="text" name="fulltext" value="${searchFilter}">
      <input type="submit" value="filter">
    </form>
    <#if searchFilter??>
      <A href="${Root.path}/${distId}/list${artifactType}s"> [ Reset ] </A>
    </#if>
    </span>
  </#if>
</#macro>
