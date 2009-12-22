<div>
<#if nxthemesInfo.model.anonymous == false && Session.hasPermission(Document.ref, "Write")>
  <div id="gadgetManager" style="display:none;" class="manager">
    <div id="gadgetManagerContainer">
      <div id="listCategories">
      <#if nxthemesInfo.model.categories>
      <#list nxthemesInfo.model.categories as category>
          <div class="button-container">
              <a class="nv-category" category="${Context.getMessage(category)}">${Context.getMessage(category)}</a>
          </div>
       </#list>
      </#if>
      </div>
      <div id="listGadgets">
        <ul class="option" id="tabGadgets">
        <#if nxthemesInfo.model.gadgets>
        <#list nxthemesInfo.model.gadgets as gadget>
          <li class="invisible">
            <div class="nameGadget">${gadget.name}</div>
            <a href="#_" name="${gadget.name}" class="typeGadget" category="${Context.getMessage(gadget.category)}" style="background-image:url(${gadget.iconUrl})"></a>
            <div class="directAdd">
              <a href="#" class="directAddLink linkAdd" name="${gadget.name}">
                  <span class="addGadgetButton">ajouter</span>
              </a>
          </div>
          </li>
       </#list>
       </#if>
      </ul>
      </div>
    </div>

  </div>
</#if>
</div>