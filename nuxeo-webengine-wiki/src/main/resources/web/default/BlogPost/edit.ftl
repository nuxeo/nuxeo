<form method="POST" action="${This.docURL}@@update" accept-charset="utf-8">
<h1><input type="text" name="dc:title" value="${Document.dublincore.title}"/></h1>
  <textarea name="bp:content" cols="75" rows="30" class="entryEdit">${This.blogPost.content}</textarea>
  
  <p class="entryEditOptions">
    Allow Trackbacks:
    <input type="radio" name="trackback" value="yes" checked /> Yes
    <input type="radio" name="trackback" value="yes" /> No
</p>

<p class="entryEditOptions">
    Allow Comments:
    <input type="radio" name="comment" value="yes" checked /> Yes
    <input type="radio" name="comment" value="yes" /> No
</p>

  <p class="buttonsGadget">
    <input type="submit" class="button"/>
  </p>
</form>
