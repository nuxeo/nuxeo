<#if (base.canWrite)>
<div class="sideblock general">
  <p class="createButton">
   <a href="${Root.urlPath}/@@create_entry"><span>Add Page!</span></a>
  </p>
</div>  
</#if>


<div class="sideblock general">
  <#include "common/nxlogin.ftl">
</div>


<!-- let other templates add more things to the sidebar -->

  <@block name="sidebar"/>

  <@block name="seeother-container" ifBlockDefined="seeother">
  <div class="sideblock contextual">
    <h3>Related documents</h3>
    <div class="sideblock-content">
      <@block name="seeother"/>
    </div>  
  </div>
  <div class="sideblock-content-bottom"></div>
  </@block>
  
  <div class="sideblock contextual">
    <h3>Toolbox</h3>
    <div class="sideblock-content">
      <ul>
        <li><a href="${This.urlPath}@@print">Print view</a></li>
        <li><a href="${This.urlPath}@@links_here">Pages that links here</a></li>
      </ul>
    </div>  
  </div>
  <div class="sideblock-content-bottom"></div>
  
<div class="sideblock general">
    <h3>Special Pages</h3>
    <ul>
      <li><a href="${Root.urlPath}/About">About this site</a></li>
      <li><a href="${Root.urlPath}/LatestChanges">Latest changes on site</a></li>
      <li><a href="${Root.urlPath}/Categories">Categories</a></li>
    </ul>
</div>  

<div class="sideblock general">
    <h3>Last Items</h3>
    <ul>
        <#list Root.document.children?reverse as entry>
            <li><a href="${Root.urlPath}/${entry.name}">${entry.title}</a></li>
            <#if entry_index = 3><#break></#if>
        </#list>
    </ul>
</div>



