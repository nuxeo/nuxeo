<@extends src="/default/Wiki/base.ftl">
<@block name="content">
<script>
$(document).ready(function(){
  $("#entry-actions > ul").tabs();
});
</script>

<!-- TS: JQuery-needed : actions in tabs, this.title under the tabs and content under this.title
    EB: DONE
-->

<div id="message">${request.getParameter('msg')}</div>

<div id="entry-actions">
  <ul>
    <li><a href="${this.docURL}@@view_content" title="page_content"><span>View</span></a></li>
    <li><a href="${this.docURL}@@edit" title="edit"><span>Edit</span></a></li>
    <li><a href="${this.docURL}@@show_versions" title="history"><span>History</span></a></li>
  </ul>
  <div id="page_content">
      <h1>${this.title}</h1>

      <@transform name="wiki">${this.wikiPage.content}</@transform>
  </div>
</div>

<div id="attached_files">
    <h3>Attached files:</h3>
    <#list this.files.files as file>
    Current File: <a href="${this.docURL}@@getfile?property=files:files/item[${file_index}]/file">${file.filename}</a>
    -
    <a href="${this.docURL}@@deletefile?property=files:files/item[${file_index}]">Remove</a>
    <br/>

    </#list>
    
    <br/>

    <form id="add_file" action="${this.docURL}@@addfile" accept-charset="utf-8" method="POST" enctype="multipart/form-data">
        <label for="file_to_add">Add a new file</label>
        <input type="file" name="files:files" value="" id="file_to_add">
        <input type="submit" name="attach_file" value="Attach" id="attach_file">
    </form>
</div>

</@block>
</@extends>
