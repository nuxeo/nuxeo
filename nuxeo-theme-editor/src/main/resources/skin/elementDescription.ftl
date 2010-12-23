<div>

<fieldset class="nxthemesEditor"><legend>Element description</legend>

<form id="nxthemesElementDescription" class="nxthemesForm" action="" onsubmit="return false">

  <div>
    <input type="hidden" name="id" value="#{selected_element.uid}" />
  </div>

  <p>
    <label>Description</label>
    <textarea name="description">${selected_element.description}</textarea>
  </p>

  <div>
    <button type="submit">Update</button>
  </div>

</form>

</fieldset>
</div>

