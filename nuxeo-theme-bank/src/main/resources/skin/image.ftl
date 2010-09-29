<@extends src="base.ftl">

  <@block name="title">
      ${collection}/${resource}
  </@block>

  <@block name="content">
    <h1>Image: ${collection}/${resource}</h1>
    <div class="imageFrame">
      <img src="${Root.getPath()}/${path}" />
    </div>
  </@block>

</@extends>