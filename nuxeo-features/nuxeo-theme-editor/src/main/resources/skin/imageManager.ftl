
<style type="text/css">
.album {
  width: 100%;
  padding: 0;
  float: left;
  background-color: #f6f6f6;
  -moz-border-radius: 8px;
}

.album .imageSingle {
  float: left;
  margin: 2px;
  width: 106px;
  text-align: center;
  border: 1px solid #999;
  -moz-border-radius: 4px;
  background-color: #fff;
}

.album .imageSingle .image {
  width: 100px;
  height: 70px;
  margin: 5px;
 }

.album .imageSingle:hover {
  border: 1px solid #666;
  cursor: pointer;
}

.album .imageSingle:hover .footer {
  color: #666;
  text-decoration: none;
}

.album .imageSingle img {
  max-width: 100px;
  max-height: 70px;
  border: none;
}
</style>

<#if current_bank>

<#if !selected_bank_collection>
  <#assign selected_bank_collection='custom' />
</#if>

<div class="window">
<div class="title">Image library</div>
<div class="body">

<table class="nxthemesManageScreen">
  <tr>
    <th style="width: 20%;">Collection</th>
    <th style="width: 80%;">Images</th>
  </tr>
  <tr>
  <td>

<ul class="nxthemesSelector">
<#list collections as collection>
  <li <#if selected_bank_collection && selected_bank_collection=collection>class="selected"</#if>><a href="javascript:NXThemesEditor.selectBankCollection('${collection}', 'image manager')">
    <img src="${basePath}/skin/nxthemes-editor/img/collection-16.png" width="16" height="16" />
    ${collection}</a></li>
</#list>
</ul>

</td>
<td>

<div>

    <div class="album" id="imageGallery">
      <#list images as image>
        <#if selected_bank_collection && selected_bank_collection=image.collection>
        <a href="javascript:void(0)" onclick="NXThemesImageManager.selectImage('${current_edit_field?js_string}', '${image.collection}/${image.name?js_string}')">
          <div class="imageSingle" title="${image.resource}">
            <div class="image"><img src="${current_bank.connectionUrl}/${image.collection}/image/${image.name}" /></div>
          </div>
        </a>
        </#if>
      </#list>
    </div>
</div>



</td>
</tr>
</table>



</div>
</div>


<div class="window">
<div class="title">Upload images</div>
<div class="body">
<div>

    <p>Uploaded images will be added to the <strong>custom</strong> collection</p>
    <iframe id="upload_target" name="upload_target" src="" style="display: none"></iframe>


     <form id="uploadImageForm" action="${current_bank.connectionUrl}/manage/upload"
          enctype="multipart/form-data" method="post" target="upload_target">
      <p>
        <input type="file" name="file" size="30" />
        <input type="hidden" name="collection" value="custom" />
        <input type="hidden" name="redirect_url" value="${Root.getPath()}/imageUploaded" />
      </p>
      <p>
      <button class="nxthemesActionButton">Upload</button>
      </p>
    </form>

</div>

</div>
</div>

  <#else>

<div class="window">
<div class="title">Image library</div>
<div class="body">

    <p>The <strong>${current_theme.name}</strong> theme is not connected to a bank.</p>
    <p>
      <a href="javascript:NXThemesEditor.manageThemeBanks()"
       class="nxthemesActionButton">Connect to a bank</a>
    </p>

</div>
</div>

</#if>


