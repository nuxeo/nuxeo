<#if (base.canWrite)>
<div class="sideblock general">
  <p class="createButton">
   <a href="${Root.urlPath}@@create_entry"><span>Add Page!</span></a>
  </p>
</div>  
</#if>

<div class="sideblock general">
  <strong>Logged in: ${base.user.isAdministrator()}</strong>
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

<div class="sideblock general">
    <h3>Last Items</h3>
    <ul>
        <#list Root.document.children?reverse as entry>
            <li><a href="${Root.urlPath}/${entry.name}">${entry.title}</a></li>
            <#if entry_index = 3><#break></#if>
        </#list>
    </ul>
</div>



