<@extends src="base.ftl">

  <@block name="title">
      ${bank}
  </@block>

  <@block name="content">
      <h1>Bank: ${bank}</h1>
      <div class="album">
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-style')">
          <div class="imageSingle">
            <div class="image"></div>
            <div class="footer">style</div>
          </div>
        </a>
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-preset')">
          <div class="imageSingle">
            <div class="image"></div>
            <div class="footer">preset</div>
          </div>
        </a>
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-image')">
          <div class="imageSingle">
            <div class="image"></div>
            <div class="footer">image</div>
          </div>
        </a>

    </div>

  </@block>

</@extends>
