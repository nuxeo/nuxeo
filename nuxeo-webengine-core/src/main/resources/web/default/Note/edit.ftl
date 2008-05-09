<@extends src="/default/base.ftl">
<@block name="content">
<h2>Edit Wiki Page : ${this.document.title} </h2>
<form method="POST" action="${this.absolutePath}@@update">
<textarea name="note:note" cols="80" rows="20">${this.document.note.note}</textarea>
<p>
Title: <input type="text" name="dc:title" value="${this.document.dublincore.title}"/>
</p>
<p>
Description: <input type="text" name="dc:description" value="${this.document.dublincore.description}"/>
</p>
<input type="submit"/>
</form>
</@block>
</@extends>
