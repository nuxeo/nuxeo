<@extends src="/default/base.ftl">
<@block name="content">
<h2>Edit Wiki Page : ${this.title} </h2>
<form method="POST" action="${this.docURL}">
<textarea name="note" cols="80" rows="20">
${this.note.note}
</textarea><br/>
<input type="submit"/>
</form>
</@block>
</@extends>
