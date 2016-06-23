<html>
<head>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
</head>
<body>
  <form action="${This.getPath()}/run" method="POST">
    <label for="names">Select reports to run:</label>
    <select name="reports" multiple="true">
<#list This.availables() as report>
     <option value="${report}" ${This.isSelected(report)?then('selected','')}>${report}</option> 
</#list>
     </select>
     <label for="dirpath">Select folder to output:</label>
     <input name="dirpath" type="folder" value="${This.dirpath()}"/>
     <label for="filename">Set the file name to output:</label>
     <input name="filename" type="text" value="${This.filename()}"/>
     <input type="submit"/>
  </form>
</body>
</html>