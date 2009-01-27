<form method="POST" action="${This.path}/@put">
<textarea name="note:note" cols="80" rows="20">${This.document.note.note}</textarea>
<p>
Title: <input type="text" name="dc:title" value="${This.document.dublincore.title}"/>
</p>
<p>
Description: <input type="text" name="dc:description" value="${This.document.dublincore.description}"/>
</p>

  <p class="entryEditOptions">
    Version increment:
    <input type="radio" name="versioning" value="major" checked> Major
    &nbsp;&nbsp;
    <input type="radio" name="versioning" value="minor"/> Minor
    &nbsp;&nbsp;
    <input type="radio" name="versioning" value=""/> None
  </p>

<p/>
<input type="submit" class="buttonsGadget"/>
</form>
