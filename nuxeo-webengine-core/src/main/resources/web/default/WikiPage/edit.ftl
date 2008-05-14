<@extends src="/default/base.ftl">
<@block name="content">
<h2>Edit Wiki Page : ${This.document.title} </h2>
<form method="POST" action="${This.urlPath}@@update">
<textarea name="wp:content" cols="80" rows="20">${This.document.wikiPage.content}</textarea>
<p>
Title: <input type="text" name="dc:title" value="${This.document.dublincore.title}"/>
</p>
<p>
Description: <input type="text" name="dc:description" value="${This.document.dublincore.description}"/>
</p>
<p>
Add File: 
</p>
<input type="submit"/>
</form>

Files:
<hr/>

<form method="POST" enctype="multipart/form-data" action="${This.urlPath}@@addfile">
<p>
<input type="file" name="files:files"/>
</p>
<input type="submit"/>
</form>
<hr/>

<#list This.document.files.files as file>
Current File: <a href="${This.urlPath}@@getfile?property=files:files/item[${file_index}]/file">${file.filename}</a>
-
<a href="${This.urlPath}@@deletefile?property=files:files/item[${file_index}]">Remove</a>
<br/>

</#list>

</@block>
</@extends>
