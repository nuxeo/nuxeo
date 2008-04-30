<@extends src="/default/base.ftl">
<@block name="content">
<h2>Edit Wiki Page : ${this.title} </h2>
<form method="POST" action="${this.docURL}@@update">
<textarea name="note:note" cols="80" rows="20">${this.note.note}</textarea>
<p>
Title: <input type="text" name="dc:title" value="${this.dublincore.title}"/>
</p>
<p>
Description: <input type="text" name="dc:description" value="${this.dublincore.description}"/>
</p>
<input type="submit"/>
</form>
</@block>
</@extends>
