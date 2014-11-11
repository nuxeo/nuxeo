<#setting url_escaping_charset='UTF-8'>

<@extends src="base.ftl">

  <@block name="header_scripts">
    <script type="text/javascript" src="${basePath}/theme-banks/skin/scripts/jquery.js"></script>
    <script type="text/javascript">
      var showImageUploadForm = function (){
        $('#uploadImageForm').show();
        $('#imageGallery').hide();
        return false;
      }
      var hideImageUploadForm = function (){
        $('#uploadImageForm').hide();
        $('#imageGallery').show();
        return false;
      }
    </script>
    
    <#assign reload=Context.request.getParameter('reload') />
    <#if reload>
      <script type="text/javascript">
          top.navtree.refresh('${bank}-${collection}-image');
      </script>
    </#if>
  </@block>

  <@block name="title">
      ${collection} image
  </@block>

  <@block name="content">
    <#assign redirect_url="${Root.getPath()}/${bank}/${collection}/image/view" />

    <h1>Image collection: ${collection}
      <a style="float: right;" href="${Root.getPath()}/${bank}/${collection}/image/view">Refresh</a>
      <#if Root.isAdministrator()>
        <a style="float: right; margin-right: 5px" href="javascript:void(0)" onclick="showImageUploadForm()">Upload image</a>
      </#if>
    </h1>

    <#if Root.isAdministrator()>
    <form style="display: none" id="uploadImageForm" action="${Root.path}/${bank}/manage/upload"
          enctype="multipart/form-data" method="post">
      <h2>Upload an image</h2>
      <p>
        <input type="file" name="file" size="30" />
        <input type="hidden" name="collection" value="${collection}" />
        <input type="hidden" name="redirect_url" value="${redirect_url?replace(' ', '%20')}?reload=1" />
      </p>
      <p>
      <button>Upload</button>
        <button onclick="hideImageUploadForm(); return false">Cancel</button>
      </p>
    </form>
    </#if>

    <div class="album" id="imageGallery">
      <#list images as image>
        <a href="javascript:void(0)" onclick="top.navtree.openBranch('${bank}-${collection}-image-${image}')">
          <div class="imageSingle">
            <div class="image"><img src="${Root.getPath()}/${bank}/${collection}/image/${image}"></div>
            <div class="footer">${image}</div>
          </div>
        </a>
      </#list>
    </div>

  </@block>

</@extends>
