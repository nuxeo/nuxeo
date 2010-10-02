<#setting url_escaping_charset='UTF-8'>

<@extends src="base.ftl">

  <@block name="title">
      ${collection} image
  </@block>

  <@block name="content">

    <#assign redirect_url="${Root.getPath()}/${bank}/image/${collection}/view" />

    <h1>Image collection: ${collection}</h1>
    <#if (Context.principal)>
    <form action="${Root.path}/upload"
          enctype="multipart/form-data" method="post">
      <p>
        <input type="file" name="file" size="40" />
        <input type="hidden" name="bank" value="${bank}" />
        <input type="hidden" name="collection" value="${collection}" />
        <input type="hidden" name="redirect_url" value="${redirect_url?replace(' ', '+')}" />
        <button>Upload image</button>
      </p>
    </form>
    </#if>

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