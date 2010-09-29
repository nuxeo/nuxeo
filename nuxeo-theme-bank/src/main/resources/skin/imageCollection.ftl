<@extends src="base.ftl">

  <@block name="title">
      ${collection} image
  </@block>

  <@block name="content">

    <h1>Image collection: ${collection}</h1>
    <form action="">
      <p>
        <input type="file" value="" size="40" />
        <button>Upload image<button>
      </p>
    </form>
    <div class="album">
      <#list images as image>
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-image-${collection}-${image}')">
          <div class="imageSingle">
            <div class="image"><img src="${Root.getPath()}/${bank}/image/${collection}/${image}"></div>
            <div class="footer">${image}</div>
          </div>
        </a>
      </#list>
    </div>

  </@block>

</@extends>