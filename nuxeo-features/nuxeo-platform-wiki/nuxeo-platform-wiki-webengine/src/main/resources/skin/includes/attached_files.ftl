<#assign files = Document.files.files />

<#if (files?size != 0 || base.canWrite)>
  <div class="attachedTitle">Attached files</div>

  <div id="attached_files">
  <#if (files?size != 0)>
    <#list files as file>
      <#if (file_index == 0)><ul></#if>
      <@compress single_line=true>
      <li><a href="${This.path}/@file?property=files:files/item[${file_index}]/file">${file.filename}</a>
        <#if (base.canWrite)>
        - <a href="${This.path}/@file/delete?property=files:files/item[${file_index}]">Remove</a>
        </#if>
      </li>
      </@compress>
      <#if (!file_has_next)></ul></#if>
    </#list>
  </#if>

  <#if base.canWrite>
    <form id="add_file" action="${This.path}/@file" accept-charset="utf-8" method="POST" enctype="multipart/form-data">
      <ul>
        <li>
          <input type="file" name="files:files" value="" id="file_to_add">
          <input type="submit" name="attach_file" value="Attach" id="attach_file">
        </li>
      </ul>
    </form>
  </#if>
  </div>

</#if>