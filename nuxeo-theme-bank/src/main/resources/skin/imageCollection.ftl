<@extends src="base.ftl">

  <@block name="title">
      ${collection} image
  </@block>

  <@block name="content">

    <h1><a href="${Root.getPath()}/${bank}/image/view">image</a> &gt; ${collection}</h1>

    <div class="album">
      <#list images as image>
        <a href="${Root.getPath()}/${bank}/image/${collection}/${image}/view">  
          <div class="imageSingle">
            <div class="image"><img src="${Root.getPath()}/${bank}/image/${collection}/${image}"></div>
            <div class="footer">${image}</div>
          </div>
        </a>
      </#list>
    </div>

  </@block>

</@extends>