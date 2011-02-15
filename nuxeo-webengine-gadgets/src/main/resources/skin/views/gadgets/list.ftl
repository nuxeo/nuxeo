<div>
    <#list gadgets as gadget>
    <div class="gadget" onclick="selectGadget('${gadget_index}','${gadget.name}')" id="gadget${gadget_index}">
    <div style="width:125px;overflow:hidden;float:left;background-color:white;">
      <#if gadget.getThumbnail()>
        <#if gadget.getThumbnail()?starts_with("http")>
            <img src="${gadget.getThumbnail()}" width="120px" height="60px"/>
        <#else>
            <img src="${Context.baseURL}${gadget.getThumbnail()}" height="60px"/>
        </#if>
      <#else>
        <img src="${skinPath}/img/default-thumb.jpg"/>
      </#if>
      </div>
      <div style="padding:2px;margin:2px">&nbsp;
      ${gadget.getTitle()}
      </div>
      <div style="clear:both"></div>

    </div>
    </#list>
</div>