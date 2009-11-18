<div>
<#if nxthemesInfo.model.anonymous == false && Session.hasPermission(Document.ref, "WRITE")>

  <div id="getLayoutManager" class="getManager">
    <a href="#" id="openLayoutManager">Modifier l'apparence</a>
  </div>
  <div id="layoutManager" style="display:none;" class="manager">
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
            <a href="#_" id="x-2-66-33" name="x-2-66-33" class="typeLayout" box="2"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-2-33-66" name="x-2-33-66" class="typeLayout" box="2"></a>
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
          <li class="invisible">
            <a href="#_" id="x-4-header3cols" name="x-4-header3cols" class="typeLayout" box="4"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-4-66-33-50-50" name="x-4-66-33-50-50" class="typeLayout" box="4"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-4-50-50-66-33" name="x-4-50-50-66-33" class="typeLayout" box="4"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-4-100-66-33-100" name="x-4-100-66-33-100" class="typeLayout" box="4"></a>
          </li>
          <li class="invisible">
            <a href="#_" id="x-4-100-33-66-100" name="x-4-100-33-66-100" class="typeLayout" box="4"></a>
          </li>
        </ul>
      </div>
    </div>

  </div>
 </#if>
</div>