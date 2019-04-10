<#macro docContent docItem>
  <div class="docContent" id="${docItem.editId}_doccontent">
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
    <#if docItem.renderingType=='html'>
        ${docItem.content}
    </#if>
    <#if docItem.renderingType=='wiki'>
        <@wiki>${docItem.content}</@wiki>
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

<#macro quickEditorLinks docItem>

<#if Root.isEditor() && docItem.getEditId()!=null >
  <img src="${skinPath}/images/edit.png" title="Edit" id='${docItem.getEditId()}_button'  onclick="return quickEditShow('${docItem.getEditId()}', '${docItem.getTarget()}')"/>
</#if>

</#macro>
<#macro inlineEdit cid contentDoc>
    <#if contentDoc==null>
     <img src="${skinPath}/images/edit.png" title="Edit" id='placeholder_${cid}_button'  onclick="return quickEditShow('placeholder_${cid}', '${cid}')"/>
      <span id="placeholder_${cid}_doctitle" ></span>
      <div id="placeholder_${cid}_doccontent" >No associated documentation</div>
    </#if>
    <#if contentDoc!=null>
      <span id="${contentDoc.getEditId()}_doctitle" >${contentDoc.title}</span>
      <@quickEditorLinks docItem=contentDoc/>
      <div class="description">
        <@docContent docItem=contentDoc />
      </div>
    </#if>
</#macro>

<#macro viewAdditionalDoc docsByCat>
  <#assign categories=docsByCat?keys/>
  <#if ((categories?size)>1)>
    <h2> Additional documentation </h2>
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

<#macro viewSecDescriptions docsByCat title=true>
  <#if docsByCat?keys?seq_contains("Description")>
    <#if title>
      <h2> Description </h2>
    </#if>
    <#list docsByCat["Description"] as docItem>
      <@docContent docItem/>
    </#list>
  </#if>
</#macro>


<#macro tableFilterArea>
<p>
Filter:
  <input name="filter" id="filter-box" value="" maxlength="30" size="30" type="text">
  <input id="filter-clear-button" type="submit" value="Clear"/>
</p>
</#macro>


<#macro tableSortFilterScript name sortList>
<script type="text/javascript">
    $(document).ready(function() {
        $("${name}")
        .tablesorter({sortList:[${sortList}], widgets:['zebra']})
        .tablesorterFilter({filterContainer: "#filter-box",
                            filterClearContainer: "#filter-clear-button",
                            filterWaitTime: 100});
    });
</script>
</#macro>
