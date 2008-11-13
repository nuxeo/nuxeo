<form method="POST" action="${This.path}/@file" enctype="multipart/form-data">
<p>
Title: <input type="text" name="dc:title" value="${This.document.dublincore.title}"/>
</p>
<p>
Description: <input type="text" name="dc:description" value="${This.document.dublincore.description}"/>
</p>
<p>
<p>
<#assign file = Document["file:content"]/>
Attached file:
<#if file.filename>
-
<a href="${This.path}/@file?property=file:content">${file.filename}</a>
-
<a href="${This.path}/@file/delete?property=file:content">Remove</a>
<#else>
None.
</#if>
</p>
<p>
Upload new: <input type="file" name="file:content" value="" id="file_to_add">
</p>
  <p class="entryEditOptions">
    Version increment:
    <input type="radio" name="versioning" value="major" checked> Major
    &nbsp;&nbsp;
    <input type="radio" name="versioning" value="minor"/> Minor
    &nbsp;&nbsp;
    <input type="radio" name="versioning" value=""/> None
  </p>
<p/>
<input type="submit" class="buttonsGadget"/>
</form>
