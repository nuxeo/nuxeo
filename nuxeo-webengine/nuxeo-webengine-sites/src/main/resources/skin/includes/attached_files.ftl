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

  <table>
    <tr><th align=left>${Context.getMessage("title.webapp.attached.files")}</th></tr>
    <#if (files?size != 0)>
      <#list files as file>
        <tr><td>
      <@compress single_line=true>        
        <img src="${skinPath}/images/${mimetypeService.getMimetypeEntryByMimeType(file.file.mimeType).iconPath}"/>
          <a href="${This.path}/@file?property=files:files/item[${file_index}]/file">${file.filename}(${file.file.length}
            <#if file.file.length &gt;999>Ko</#if>
            <#if file.file.length &lt;=999>B</#if>)</a>
            <#if (base.canWrite)>
              - <a href="${This.path}/@file/delete?property=files:files/item[${file_index}]">Remove</a>
            </#if>
      </@compress>
        </td></tr>
      </#list>
    </#if> 
  </table> 
  
  <#if base.canWrite>
    <form id="add_file" name="add_file" action="${This.path}/@file" accept-charset="utf-8" method="POST" enctype="multipart/form-data" >
      <table><tr>
        <td>
          <a onclick="return validateAttachedFile();"><img src="${skinPath}/images/action_add.gif" alt="${Context.getMessage("title.webapp.attached.files.attach.action")}" title="${Context.getMessage("title.webapp.attached.files.attach.action")}"></a>

          <input type="file" name="files:files" value="" id="file_to_add" required="true"/>
        </td>      
      </tr></table> 
    </form>
  </#if>      
      
</#if>