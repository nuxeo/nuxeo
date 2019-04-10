
  <form action="${This.path}/run" method="POST">
    <label for="names">Select reports to run:</label>
    <select name="reports" multiple="true">
<#list This.availables() as report>
     <option value="${report}" ${This.isSelected(report)?then('selected','')}>${report}</option> 
</#list>
     </select>
     and then <input type="submit" value="run"/>
  </form>
