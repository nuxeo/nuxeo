<!-- Base template that defines the site layout -->
<html>
  <head>
    <title>Sample4 - Adapter example</title>
  </head>
  <body>
    <#-- Look here how $This is used to access current user and not Buddies adapter -->
    <h3>Buddies for user ${This.name}!</h3>
    <#-- Look here how to access the adapter instance: ${This.activeAdapter} -->
    This is an adapter named  ${This.activeAdapter.name}
    <ul>
    Buddies:
        <li><a href="${This.previous.path}/user/Tom">Tom</li>
        <li><a href="${This.previous.path}/user/Jerry">Jerry</li>
    </ul>
  </body>
</html>

