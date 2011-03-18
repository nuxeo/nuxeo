<div>
    <#list gadgets as gadget>
    <div class="gadget" id="gadget${gadget_index}">
        <div class="gadgetThumb">
      <#if gadget.getThumbnail()>
        <#if gadget.getThumbnail()?starts_with("http")>
            <img src="${gadget.getThumbnail()}" height="64px"/>
        <#else>
            <img src="${gadget.getThumbnail()}" height="64px"/>
        </#if>
      <#else>
        <img src="${skinPath}/img/default-thumb.png" height="64px"/>
      </#if>
      </div>

      <input type="button" value="add" class="addGadget" onclick="doAddGadget('${gadget.name}','${gadget.getGadgetDefinition().toString()}')"/>

      <h3>${gadget.title}</h3>

      <p class="author">${gadget.author}</p>

      <p class="description">${gadget.description}</p>

      <p class="specUrl"><a href="${gadget.getPublicGadgetDefinition()}">${Context.getMessage('label.gadget.url')}</a></p>

      <div style="clear:both"></div>

    </div>
    </#list>
</div>
