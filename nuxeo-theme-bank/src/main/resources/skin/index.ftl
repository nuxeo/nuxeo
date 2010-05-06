<html>
<head>
  <title>Nuxeo Theme Bank</title>
</head>

<body>
  <h1>Nuxeo Theme Bank</h1>

  <ul>
    <#list Root.getBankNames() as bank>
      <li><a href="${Root.getPath()}/${bank}">${bank}</a></li>
    </#list>
  </ul>

</body>
</html>