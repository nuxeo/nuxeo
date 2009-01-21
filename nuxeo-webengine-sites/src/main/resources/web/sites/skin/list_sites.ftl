<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
<title>Sites</title>
</head>
<body>
Sites available
<hr>
<#list sites as s>
<a href="${This.path}/${s.href}"> ${s.name} </a>
<br>
</#list>
</body>
</html>
