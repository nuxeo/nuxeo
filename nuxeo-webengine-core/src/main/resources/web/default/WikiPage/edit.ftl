<@extends src="/default/base.ftl">
<@block name="content">
<h2>Edit Wiki Page : ${this.document.title} </h2>
<form method="POST" action="${this.urlPath}@@update">
<textarea name="wp:content" cols="80" rows="20">${this.document.wikiPage.content}</textarea>
<p>
Title: <input type="text" name="dc:title" value="${this.document.dublincore.title}"/>
</p>
<p>
Description: <input type="text" name="dc:description" value="${this.document.dublincore.description}"/>
</p>
<p>
Add File: 
</p>
<input type="submit"/>
</form>

Files:
<hr/>

<form method="POST" enctype="multipart/form-data" action="${this.urlPath}@@addfile">
<p>
<input type="file" name="files:files"/>
</p>
<input type="submit"/>
</form>
<hr/>

<#list this.document.files.files as file>
Current File: <a href="${this.urlPath}@@getfile?property=files:files/item[${file_index}]/file">${file.filename}</a>
-
<a href="${this.urlPath}@@deletefile?property=files:files/item[${file_index}]">Remove</a>
<br/>

</#list>

</@block>
</@extends>
