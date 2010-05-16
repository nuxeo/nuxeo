<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1><a href="${Root.getPath()}/${bank}/preset/view">preset</a> ${collection}</h1>
    <div class="album">
      <#list presets as preset>
        <a href="${Root.getPath()}/${bank}/preset/${collection}/${preset}/view">
          <div class="imageSingle">   
            <div class="image"><img src="${Root.getPath()}/${bank}/preset/${collection}/${preset}/preview"></div>
            <div class="footer">${preset}</div>
          </div>
        </a>
      </#list>
    </div>
  </@block>

</@extends>
