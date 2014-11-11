<html>
<head>
<title>Nuxeo Server Management</title>
</head>
<body>
	<h2>${This.runtime.getProperty("org.nuxeo.ecm.product.name")}</h2>
	<h3>${This.runtime.getProperty("org.nuxeo.ecm.product.version")}</h3>
	<p>
		<div>Home Directory: ${This.environment.home}</div>
		<div>Data Directory: ${This.environment.data}</div>
		<div>Log Directory: ${This.environment.log}</div>
		<div>Web Directory: ${This.environment.web}</div>
		<div>Temp. Directory: ${This.environment.temp}</div>
	</p>

	<p>
 	<a href="bundles">Bundles</a>
 	<br>
 	<a href="components">Components</a>
 	<br>
 	<a href="resources">Resources</a>
	</p>
</body>
</html>
