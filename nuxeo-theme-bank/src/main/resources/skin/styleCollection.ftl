<html>
<head>
  <title>Nuxeo Theme Bank</title>
</head>

<body>
  <h1>Style</h1>

  <ul>
    <#list styles as style>
      <li><a href="${Root.getPath()}/${bank}/style/${collection}/${style}">${style}</a></li>
    </#list>
  </ul>
  
</body>
</html>