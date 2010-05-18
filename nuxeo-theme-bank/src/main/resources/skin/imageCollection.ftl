<@extends src="base.ftl">

  <@block name="title">
      ${collection} image
  </@block>

  <@block name="content">

    <h1><a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}')">${bank}</a> &gt; 
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-image')">image</a> &gt; 
        <span>${collection}</span></h1>
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