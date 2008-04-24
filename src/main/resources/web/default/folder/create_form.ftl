<@extends src="/default/base.ftl">
<@block name="content">
<h2>Create Wiki Page : ${this.title} </h2>
<form method="POST" action="${this.docURL}@@update">
<textarea name="note:note" cols="80" rows="20"></textarea>
<p>
Title: <input type="text" name="dc:title" value="${context.getFirstUnresolvedSegment()}"/>
</p>
<p>
Description: <input type="text" name="dc:description" value=""/>
</p>
<input type="submit"/>
</form>
</@block>
</@extends>
