<div>
<#if nxthemesInfo.model.anonymous == false && Session.hasPermission(Document.ref, "WRITE")>
  <style>
  #tabnav {
    border-bottom:1px solid grey;
    margin:0;
    padding:0 0 10px 10px;
    background-color:#D6D5D3;
  }

  #tabManager{
    background-color:#D6D5D3;
  }
  #tabnav li {
    display:inline;
    list-style-type:none;
    margin:0;
    padding:0;
  }
  .tabManage {
    font-size:10px;
    font-weight:bold;
    line-height:14px;
    margin:0 10px 4px;
    text-decoration:none;
  }
  #tabnav li.active a:link, #tabnav li.active a:visited, #tabnav a:hover {
    background:transparent none repeat scroll 0 0;
    border-bottom: 2px solid #B0C830;
    color:white;
    cursor:pointer;
  }

  .selected{
    background:transparent none repeat scroll 0 0;
    color:white;
    cursor:pointer;
  }

  #tabnav a:hover {
    color:#B0C830;
  }
  </style>
  <div id="tabManager"><a href="#" id="openTabsManager">Modifier l'onglet</a></div>

  <div id="tabManagerOpen" style="display:none;">
    <div id="closeLayoutManager">
        <a href="#" id="closeLinkLayoutManager">Fermer</a>
    </div>

    <ul id="tabnav">
       <li class="active"><a class="tabManage selected" id="#layoutManager">Modifier l'apparence</a></li>
       <li><a class="tabManage" id="#gadgetManager">Ajouter un gadget</a></li>
       <li><a class="tabManage" id="#ThemeManager">Modifier le th&egrave;me</a></li>
    </ul>

  <div id="layoutManager" style="display:none;" class="manager">
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