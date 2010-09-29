<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1>Preset collection: ${collection}</h1>
    <div class="album">
      <#list presets as preset>
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-preset-${collection}-${preset}')">
          <div class="imageSingle">
            <div class="image"><img src="${Root.getPath()}/${bank}/preset/${collection}/${preset}/preview"></div>
            <div class="footer">${preset}</div>
          </div>
        </a>
      </#list>
    </div>
  </@block>

</@extends>
