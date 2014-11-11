<div>
<#import "common/util.ftl" as base/>
<#assign files = Document.files.files />

<script type="text/javascript">
  function validateAttachedFile(){
    var attachedFile=document.getElementById('file_to_add');
    if("" == attachedFile.value) {
      alert('You need to browse for a document !');
      return false;
    }
    document.add_file.submit();
  }
</script>

<#if (files?size != 0 || base.canWrite)>
<div class="attachedFileBlock">
  <h4>${Context.getMessage("title.webapp.attached.files")}</h4>
    <#if (files?size != 0)>
      <#list files as file>
        <div class="attachedFileInfo">
      <@compress single_line=true>
        <img src="${contextPath}/icons/${mimetypeService.getMimetypeEntryByMimeType(file.file.mimeType).iconPath}"/>
          <a href="${This.path}/@file?property=files:files/item[${file_index}]/file">${file.filename}(${file.file.length}
            <#if file.file.length &gt;999>Ko</#if>
            <#if file.file.length &lt;=999>B</#if>)</a>
            <#if (base.canWrite)>
              - <a href="${This.path}/@file/delete?property=files:files/item[${file_index}]">Remove</a>
            </#if>
      </@compress>
        </div>
      </#list>
    </#if>


  <#if base.canWrite>
    <form id="add_file" name="add_file" action="${This.path}/@file" accept-charset="utf-8" method="POST" enctype="multipart/form-data" >
      <div class="addFile">
          <input type="file" name="files:files" value="" id="file_to_add" required="true"/>
      </div>
          <input type="button" class="button" value="${Context.getMessage("title.webapp.attached.files.attach.action")}" onclick="return validateAttachedFile();" />
    </form>
  </#if>
 </div>
</#if>
</div>
