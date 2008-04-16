<html>
<body>
<h1>Wiki: ${this.title}</h1>

<@block name="content"/>

<hr>
engine : ${env.engine} ${env.version}

<hr>
<#--
<#list this.session.query('SELECT * FROM Folder ORDER BY dc:modfied') as doc>
  ${doc.title}<br/>
</#list>
-->
</body>
</html>
