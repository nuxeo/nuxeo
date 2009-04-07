<div id="webPageEdit">

  <form action="${This.path}/@put" method="POST">
    <table class="formFill">
      <tbody>
        <tr>
          <td>Title:</td>
          <td><input type="text" name="dc:title" value="${Document.title}" size="40"/></td>
        </tr>
        <tr>
          <td colspan="2">Description</td>
        </tr>
        <tr>
          <td colspan="2"><textarea name="dc:description" cols="54">${Document["dc:description"]}</textarea></td>
        </tr>
        <tr>
          <td colspan="2" align="right"><input type="submit" value="Save" class="buttonsGadget"/></td>
        </tr>
      </tbody>
    </table>
  </form>
</div>
