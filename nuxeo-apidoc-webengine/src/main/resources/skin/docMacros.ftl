<#macro docContent docItem>
<div class="docContent" id="${docItem.editId}_doccontent">
  <#if docItem.applicableVersion??>
    <#if ((docItem.applicableVersion?size)>0) >
      <div class="docVersionDisplay">
        <ul>
          <#list docItem.applicableVersion as version>
            <li class="sticker">${version}</li>
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
  <img src="${skinPath}/images/edit.png" title="Edit" id='${docItem.getEditId()}_button'
    onclick="return quickEditShow('${docItem.getEditId()}', '${docItem.getTarget()}')"/>
  </#if>

</#macro>
<#macro inlineEdit cid contentDoc>
  <#if contentDoc==null>
  <img src="${skinPath}/images/edit.png" title="Edit" id='placeholder_${cid}_button'
    onclick="return quickEditShow('placeholder_${cid}', '${cid}')"/>
  <span id="placeholder_${cid}_doctitle"></span>
  <div id="placeholder_${cid}_doccontent">No associated documentation</div>
  </#if>
  <#if contentDoc!=null>
  <span id="${contentDoc.getEditId()}_doctitle">${contentDoc.title}</span>
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
                <li><a href="javascript:showAddDoc('${docItem.id}')">${docItem.title}</a></li>
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
        <a name="${docItem.id}"> ${docItem.title} </a>
        <@docContent docItem/>
      </div>
      </#list>
    </#list>
  </#if>
</#macro>

<#macro viewSecDescriptions docsByCat title=true>
  <#if docsByCat?keys?seq_contains("Description")>
    <#list docsByCat["Description"] as docItem>
      <@docContent docItem/>
    </#list>
  </#if>
</#macro>

<#macro fulltextFilter name action>
  <#if false>
  <!--
  params:
   - name: Displayed name in the placeholder.
   - action: name of the resource that can make a fulltext search; for instance `listExtensionPoints`
             from `org.nuxeo.apidoc.browse.ApiBrowser#filterExtensionPoints`
  -->
  </#if>
  <form id="fulltext" method="POST" action="${Root.path}/${distId}/${action}">
    <input name="fulltext" id="fulltext-box" size="30" type="search"
    placeholder="Find in ${name}"<#if searchFilter??> value="${searchFilter}"</#if>/>
    <input id="filter-submit-button" type="submit" value="Search"/>
  </form>
</#macro>

<#macro tableFilterArea name>
  <#if false>
  <!--
  params:
   - name: Displayed name in the placeholder
  -->
  </#if>
  <input name="fulltext" id="filter-box" size="30" type="search" placeholder="Filter ${name}"/>
</#macro>


<#macro googleSearchFrame criterion>
<div class="googleSearchFrame">
  <script>
    (function() {
      var render = function() {
        var params = {
          div: 'gsr',
          tag: 'searchresults-only',
          gname: 'gsr'
        };

        google.search.cse.element.render(params);
        google.search.cse.element.getElement(params.gname).execute('${criterion?js_string}');
      };

      var gseCallback = function() {
        if (document.readyState === 'complete') {
          render();
        } else {
          google.setOnLoadCallback(render, true);
        }
      };

      window.__gcse = {
        parsetags: 'explicit',
        callback: gseCallback
      };

      var cx = '012209002953539942917:sa7nmpwkglq';
      var gcse = document.createElement('script');
      gcse.type = 'text/javascript';
      gcse.async = true;
      gcse.src = 'https://cse.google.com/cse.js?cx=' + cx;
      var s = document.getElementsByTagName('script')[0];
      s.parentNode.insertBefore(gcse, s);
    })();
  </script>
  <div id="gsr"></div>
</div>
</#macro>

<#macro tableSortFilterScript name sortList>
<script type="text/javascript">
  $(document).ready(function() {
    $("${name}")
    .tablesorter({sortList: [${sortList}], widgets: ['zebra'], cancelSelection: false})
      <#if !searchFilter??>.tablesorterFilter({
        filterContainer: "#filter-box",
        filterClearContainer: "#filter-clear-button",
        filterWaitTime: 600
      }</#if>);
  });

  $('#filter-box').click(function(e) {
    e.stopPropagation();
  });

  $('#fulltext').children().click(function(e) {
    e.stopPropagation();
  });
</script>
</#macro>
