<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
<title>Wiki</title>
</head>
<body>
<H1>WIKI template</H1>
<hr>
Logo : <img src="${This.path}/logo" alt="logo">
<hr>

Welcome Animation/Image</br>
<img src="${This.path}/welcomeMedia" alt="Welcome Media">

<hr>
Welcome Text:</br>
${welcomeText}


<hr>
<#include "includes/tree.ftl"/>
<@navigator/>

</body>
</html>
