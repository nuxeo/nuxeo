<#assign files = Document.files.files />

 <script type="text/javascript">
 function validateAttachedFile(){
 
   var attachedFile=document.getElementById('file_to_add');
   if("" == attachedFile.value) {
     return false;
   }
   return true;     
 }
 </script>

<#if (files?size != 0 || base.canWrite)>

  <table>
    <tr><th align=left>${Context.getMessage("title.webapp.attached.files")}</th></tr>
    <#if (files?size != 0)>
      <#list files as file>
        <tr><td>
      <@compress single_line=true>
        <a href="${This.path}/@file?property=files:files/item[${file_index}]/file">${file.filename}(${file.file.length}Ko)</a>
            <#if (base.canWrite)>
              - <a href="${This.path}/@file/delete?property=files:files/item[${file_index}]">Remove</a>
            </#if>
      </@compress>
        </td></tr>
      </#list>
    </#if> 
  <table> 
  
  <#if base.canWrite>
    <form id="add_file" action="${This.path}/@file" accept-charset="utf-8" method="POST" enctype="multipart/form-data" onsubmit="return validateAttachedFile()">
      <table><tr>
        <td>
          <input type="file" name="files:files" value="" id="file_to_add" required="true">
          <input type="submit" name="attach_file" value="Attach" id="attach_file" >
        </td>      
      </tr></table> 
    </form>
  </#if>      
      
</#if>