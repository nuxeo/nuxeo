<@extends src="/default/Wiki/base.ftl">
<@block name="content">

<h2>Edit Wiki Page : ${this.title} </h2>
<form method="GET" action="${this.docURL}@@update">
<textarea name="wp:content" cols="75" rows="30">${this.wikiPage.content}</textarea>
<p>
Title: <input type="text" name="dc:title" value="${this.dublincore.title}"/>
</p>

<p>
    Version increment:
    <input type="radio" name="versioning" value="major" checked> Major
    &nbsp;&nbsp;
    <input type="radio" name="versioning" value="minor"/> Minor
</p>

<input type="submit"/>
</form>
</@block>
</@extends>
