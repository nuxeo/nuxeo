<div>
<#if nxthemesInfo.model.anonymous == false>

  <div id="getLayoutManager">
    <a href="#" id="openLayoutManager">Modifier l'apparence</a>
  </div>
  <div id="layoutManager" style="display:none;">
    <div id="closeLayoutManager">
      <a href="#" id="closeLinkLayoutManager">Fermer</a>
    </div>
    <div id="layoutManagerContainer">
      <div id="listColumns">
        <div class="button-container">
          <button class="nv-layout" box="1">1</button>
        </div>
        <div class="button-container">
          <button class="nv-layout" box="2">2</button>
        </div>
        <div class="button-container">
          <button class="nv-layout" box="3">3</button>
        </div>
        <div class="button-container">
          <button class="nv-layout" box="4">4</button>
        </div>
      </div>
      <div id="listLayouts">
        <ul class="option" id="tabLayout">
          <li class="invisible">
            <a href="#_" id="x-1-default" name="x-1-default" class="typeLayout" box="1"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-2-default" name="x-2-default" class="typeLayout" box="2"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-3-default" name="x-3-default" class="typeLayout" box="3"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-3-header2cols" name="x-3-header2cols" class="typeLayout" box="3"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-4-footer3cols" name="x-4-footer3cols" class="typeLayout" box="4"></a>
          </li>
        </ul>
      </div>
    </div>

  </div>
 </#if>
</div>