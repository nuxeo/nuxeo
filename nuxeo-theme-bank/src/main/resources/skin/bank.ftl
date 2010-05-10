<html>
<head>
  <title>Nuxeo Theme Bank</title>
</head>

<body>
  <h1>Nuxeo Theme Bank</h1>

  <h2>Style collections</h2>
  <ul>
    <#list styleCollections as collection>
      <li><a href="${Root.getPath()}/${bank}/style/${collection}">${collection}</a></li>
    </#list>
  </ul>
  
  <h2>Image collections</h2>
  <ul>
    <#list imageCollections as collection>
      <li><a href="${Root.getPath()}/${bank}/image/${collection}">${collection}</a></li>
    </#list>
  </ul>
  
</body>
</html>