
<form method="POST" action="${This.path}/createDocumentation"  enctype="application/x-www-form-urlencoded">

<table width="90%">
<tr><td>
  <table>
  <tr>
    <td> Type : </td>
    <td> <input type="text" name="type" value="${docItem.type}"/> </td>
  </tr>
  <tr>
    <td> Title : </td>
    <td> <input type="text" name="title" value="${docItem.title}" size="80"/> </td>
  </tr>
  <tr>
    <td> Content : </td>
    <td>
     <textarea name="content" cols="80" rows="20">
     ${docItem.content}
    </textarea>
    </td>
  </tr>
  </table>
</td>
<td>
  <table>
  <tr>
      <td> Approved by Nuxeo : </td>
      <td> <input type="checkbox" name="approved"/> </td>
  </tr>
  <tr>
    <td> Applicable versions : </td>
    <td> <select size="3" name="versions" multiple="multiple">
    <option value="V1">V1</option>
    <option value="V2">V2</option>
    <option value="V3">V3</option>
    </select>
   </td>
  </tr>
  </table>
</td>
</tr>
</table>

<input type="text" name="id" value="${docItem.id}"/>
<input type="text" name="uuid" value="${docItem.uUID}"/>
<input type="text" name="targetType" value="${docItem.targetType}"/>
<input type="text" name="target" value="${docItem.target}"/>

<input type="submit"/>

</form>