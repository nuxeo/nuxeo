<html>
<head>

</head>
<body>
<h1>Oups</h1>

<p>
<#if exceptionMsg??>
  Your invitation cannot be validated : "${exceptionMsg}".
</#if>
<#if error??>
  An error occured during your invitation processs.
</#if>
</p>
</body>
</html>