<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>${collection}</title>
    <link type="text/css" rel="stylesheet" href="${skinPath}/styles/ui.css" />
</head>
<body>

<h1>${collection}</h1>

  <ul>
    <#list presets as preset>
      <li><a href="${Root.getPath()}/${bank}/preset/${collection}/${preset}">${preset}</a></li>
    </#list>
  </ul>

</body>
</html>
