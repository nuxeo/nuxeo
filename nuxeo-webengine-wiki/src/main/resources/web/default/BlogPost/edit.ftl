<form method="POST" action="${this.docURL}@@update" accept-charset="utf-8">
<h1><input type="text" name="dc:title" value="${this.dublincore.title}"/></h1>
  <textarea name="bp:content" cols="75" rows="30" class="wikiEdit">${this.blogPost.content}</textarea>

  <p class="buttonsGadget">
    <input type="submit" class="button"/>
  </p>
</form>
