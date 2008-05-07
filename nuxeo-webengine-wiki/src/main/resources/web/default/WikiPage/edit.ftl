<h1>Edit Wiki Page : ${this.title} </h1>
<form method="POST" action="${this.docURL}@@update" accept-charset="utf-8">
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