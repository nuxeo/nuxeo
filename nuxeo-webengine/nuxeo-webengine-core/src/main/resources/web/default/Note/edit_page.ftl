<form method="POST" action="${This.urlPath}@@update">
<textarea name="note:note" cols="80" rows="20">${This.document.note.note}</textarea>
<p>
Title: <input type="text" name="dc:title" value="${This.document.dublincore.title}"/>
</p>
<p>
Description: <input type="text" name="dc:description" value="${This.document.dublincore.description}"/>
</p>
<input type="submit"/>
</form>
