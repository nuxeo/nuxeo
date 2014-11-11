<form method="POST" name="operationForm" action="${This.path}/executeOperation">

Import operation :  <select name="operationId" onchange="if (this.value=='Import with metadata') {document.getElementById('metadata').style.display='block'} else {document.getElementById('metadata').style.display='none'};">
    <#list operations as operation>
      <option value="${operation}">${operation}</option>
    </#list>
  </select>

 <div style="display:none" id="metadata">
   <table>
      <tr>
        <td>Description</td>
        <td><input name="dc:description"></td>
      </tr>
      <tr>
        <td>Subject</td>
        <td><input name="dc:subject"></td>
      </tr>
   </table>
  </div>

 <input type="hidden" name="batchId" value="${batchId}"/>
 <input type="hidden" name="context" value="${context}"/>

</form>