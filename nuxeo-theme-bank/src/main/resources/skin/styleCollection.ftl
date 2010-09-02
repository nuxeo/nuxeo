<@extends src="base.ftl">

  <@block name="title">
      ${collection}
  </@block>

  <@block name="content">
    <h1><a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}')">${bank}</a> &gt;
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-style')">style</a> &gt;
        <span>${collection}</span></h1>

    <div class="album">
      <#list styles as style>
        <#assign style_info=info[style] />
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-style-${collection}-${style}')">
        <div class="imageSingle">
          <div class="image"><img src="${Root.getPath()}/${bank}/style/${collection}/${style}/preview"></div>
          <div class="footer"><div>${style}
            <#if style_info>
               (${style_info.description})
            </#if>
            </div>
          </div>
        </div>
        </a>
      </#list>
    </div>

  </@block>

</@extends>
