<@extends src="base.ftl">

  <@block name="title">
      ${collection}/${resource}
  </@block>

  <@block name="content">
    <h1><a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}')">${bank}</a> &gt;
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-image')">image</a> &gt;
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-image-${collection}')">${collection}</a>/<span>${resource}</span></h1>
    <div class="imageFrame">
      <img src="${Root.getPath()}/${path}" />
    </div>
  </@block>

</@extends>