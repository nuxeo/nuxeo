<@extends src="base.ftl">

  <@block name="title">
      ${collection}/${resource}
  </@block>

  <@block name="content">
    <h1><a href="${Root.getPath()}/${bank}/image/view">image</a> &rarr; <a href="${Root.getPath()}/${bank}/image/${collection}/view">${collection}</a>/${resource}</h1>
    <div class="imageFrame">
      <img src="${Root.getPath()}/${path}" />
    </div>
  </@block>

</@extends>