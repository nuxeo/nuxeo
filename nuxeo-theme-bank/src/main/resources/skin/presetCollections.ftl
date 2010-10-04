<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1>Presets
      <a style="float: right" href="${Root.getPath()}/${bank}/preset/view">Refresh</a>
    </h1>
    <div class="album">
      <#list collections as collection>
                <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-preset-${collection}')">
          <div class="imageSingle">
            <div class="image"><img src="${basePath}/theme-banks/skin/img/collection.png"></div>
            <div class="footer">${collection}</div>
          </div>
        </a>
      </#list>
    </div>

  </@block>

</@extends>