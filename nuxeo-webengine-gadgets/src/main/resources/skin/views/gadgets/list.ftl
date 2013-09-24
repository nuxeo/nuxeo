<div>
  <#list gadgets as gadget>
  <div class="gadget" gadget-name="${gadget.name}" gadget-spec-url="${gadget.getGadgetDefinition().toString()}">
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

    <h3>${gadget.getTitle(This.context.locale)}</h3>
    <p class="author">${gadget.author}</p>
    <p class="description">${gadget.getDescription(This.context.locale)}</p>
    <p class="specUrl"><a href="${gadget.getPublicGadgetDefinition()}">${Context.getMessage('label.gadget.url')}</a></p>
    <div style="clear:both"></div>
  </div>
  </#list>
</div>
