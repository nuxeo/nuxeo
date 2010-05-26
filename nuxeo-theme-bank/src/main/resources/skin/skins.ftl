<@extends src="base.ftl">

  <@block name="title">
  </@block>

  <@block name="content">
  
    <div class="album">
      <#list skins as skin>
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${skin.bank}-style-${skin.collection}-${skin.name}')">
          <div class="imageSingle">   
            <div class="image"><img src="${Root.getPath()}/${skin.bank}/style/${skin.collection}/${skin.name}/preview"></div>
            <div class="footer">${skin.name}</div>
          </div>
        </a>
      </#list>
    </div>
  </@block>

</@extends>
