<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>${resource}</title>

    <link type="text/css" rel="stylesheet" href="${skinPath}/styles/ui.css" />

    <script type="text/javascript" src="${skinPath}/scripts/syntaxHighlighter/shCore.js"></script>
    <script type="text/javascript" src="${skinPath}/scripts//syntaxHighlighter/shBrushCss.js"></script>
    <link type="text/css" rel="stylesheet" href="${skinPath}/scripts/syntaxHighlighter/shCore.css"/>
    <link type="text/css" rel="stylesheet" href="${skinPath}/scripts/syntaxHighlighter/shThemeDefault.css"/>

    <script type="text/javascript">
        SyntaxHighlighter.all();
    </script>
</head>

<body >

<h1>${resource}</h1>

<pre style="visibility: hidden" class="brush: css; toolbar: false">
${content}
</pre>

</body>
</html>
