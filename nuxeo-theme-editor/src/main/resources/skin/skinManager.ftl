
<#assign saveable=current_theme && current_theme.saveable>

<#if current_bank>

<#if current_base_skin_name>

<div class="window">
<div class="title">Top layer</div>
<div class="body">
  <#if skins>
    <div style="padding: 10px 5px">
    <div>Add a top layer skin to the <strong>${current_theme.name}</strong> theme:
    ${current_skin_name}
    </div>
    <#list skins as skin>
      <div class="nxthemesImageSingle nxthemesImageSingle<#if current_skin_name=skin.name>Selected</#if>">
        <a href="javascript:<#if saveable>NXThemesSkinManager.activateSkin('${current_theme.name}', '${skin.bank}', '${skin.collection}', '${skin.resource?replace('.css', '')}', false)<#else>void(0)</#if>">
          <img src="${current_bank.connectionUrl}/${skin.collection}/style/${skin.resource}/preview" />
          <div>${skin.name}</div>
        </a>
      </div>
    </#list>

    <div style="clear: both"></div>
    </div>
  <#else>
    <p>No skins available</p>
  </#if>
</div>
</div>

</#if>

<div class="window">
<div class="title">Base skin</div>
<div class="body">
  <#if base_skins>
    <div style="padding: 10px 5px">
    <div>Select a base skin for the <strong>${current_theme.name}</strong> theme:
     <#if current_skin_name>
      <button style="float: right" class="nxthemesActionButton" 
      onclick="javascript:NXThemesSkinManager.deactivateSkin('${current_theme.name?js_string}')">Remove skin</button>
    </#if>
    </div>
    <#list base_skins as skin>
      <div class="nxthemesImageSingle nxthemesImageSingle<#if current_base_skin_name=skin.name>Selected</#if>">
        <a href="javascript:<#if saveable>NXThemesSkinManager.activateSkin('${current_theme.name}', '${skin.bank}', '${skin.collection}', '${skin.resource?replace('.css', '')}', true)<#else>void(0)</#if>">
          <img src="${current_bank.connectionUrl}/${skin.collection}/style/${skin.resource}/preview" />
          <div>${skin.name}</div>
        </a>
      </div>
    </#list>
    <div style="clear: both"></div>
    </div>
  <#else>
    <p>No base layers available</p>
  </#if>
</div>
</div>


<#else>

<div class="window">
<div class="title">Skins</div>
<div class="body">

    <p>The <strong>${current_theme.name}</strong> theme is not connected to a bank.</p>
  <p>
    <a href="javascript:NXThemesEditor.manageThemeBanks()"
       class="nxthemesActionButton">Connect to a bank</a>
  </p>
  
</div>
</div>
</#if>

<#if !saveable>
  <div id="nxthemesTopBanner" style="position: absolute">
    <div class="nxthemesInfoMessage">
      <img src="${basePath}/skin/nxthemes-editor/img/error.png" width="16" height="16" style="vertical-align: bottom" />
      <span>Before you can select a skin you need to customize the <strong>${current_theme.name}</strong> theme.</span>
      <button class="nxthemesActionButton"
       onclick="NXThemesEditor.customizeTheme('${current_theme.src?js_string}', 'skin manager')">Customize theme</button>
    </div>
  </div>   
</#if>
