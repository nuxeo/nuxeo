<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1><a href="${Root.getPath()}/${bank}/view">${bank}</a> &gt; style</h1>
    <div class="album">
      <#list collections as collection>
        <a href="${Root.getPath()}/${bank}/style/${collection}/view">  
          <div class="imageSingle">
            <div class="image"></div>
            <div class="footer">${collection}</div>
          </div>
        </a>
      </#list>
    </div>

  </@block>

</@extends>