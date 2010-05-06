<html>
<head>
  <title>Nuxeo Theme Bank</title>
</head>

<body>
  <h1>Nuxeo Theme Bank</h1>

  <h2>Styles</h2>
  <ul>
    <#list styleCollections as collection>
      <li><a href="${Root.getPath()}/${bank}/${collection}">${collection}</a></li>
    </#list>
  </ul>
  
</body>
</html>