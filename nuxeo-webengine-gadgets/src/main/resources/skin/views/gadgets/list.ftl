<div>
    <#list gadgets as gadget>
    <div class="gadget" id="gadget${gadget_index}">
        <div class="gadgetThumb">  
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
      
      <input type="button" value="add" class="addGadget" onclick="doAddGadget('${gadget.name}','${gadget.getGadgetDefinition().toString()}')"/>
       
      <h3>${gadget.title}</h3>
      
      <p class="author">${gadget.author}</p>
      
      <p class="description">${gadget.description}</p>
 
      <div style="clear:both"></div>

    </div>
    </#list>
</div>
