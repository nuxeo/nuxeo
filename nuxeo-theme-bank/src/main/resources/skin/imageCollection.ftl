<html>
<head>
  <title>Nuxeo Theme Bank</title>
</head>

<body>
  <h1>${collection} images</h1>

  <ul>
    <#list images as image>
      <li><a href="${Root.getPath()}/${bank}/image/${collection}/${image}">${image}</a></li>
    </#list>
  </ul>
  
</body>
</html>