<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1>Presets</h1>
    <div class="album">
      <#list collections as collection>
                <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-preset-${collection}')">
          <div class="imageSingle">
            <div class="image"></div>
            <div class="footer">${collection}</div>
          </div>
        </a>
      </#list>
    </div>

  </@block>

</@extends>