<@extends src="base.ftl">

  <@block name="title">
      ${collection}/${resource}
  </@block>


  <@block name="content">
    <h1>Image: ${collection}/${resource}
      <a style="float: right" href="${Root.getPath()}/${bank}/${collection}/image/${resource}/view">Refresh</a>
    </h1>

    <div class="imageFrame" id="imageFrame">
      <img src="${Root.getPath()}/${bank}/${collection}/image/${resource}" />
    </div>

  </@block>

</@extends>