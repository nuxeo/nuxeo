<form method="POST" action="${This.urlPath}@@update" accept-charset="utf-8">
<h1><input type="text" name="dc:title" value="${Document.dublincore.title}"/></h1>
  <textarea name="wp:content" cols="75" rows="30" class="entryEdit">${Document.wikiPage.content}</textarea>
  <p class="entryEditOptions">
    Version increment:
    <input type="radio" name="versioning" value="major" checked> Major
    &nbsp;&nbsp;
    <input type="radio" name="versioning" value="minor"/> Minor
  </p>
  <p class="buttonsGadget">
    <input type="submit" class="button"/>
  </p>
</form>
