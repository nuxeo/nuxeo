<?xml version="1.0"?>
<service xmlns="http://www.w3.org/2007/app" xmlns:nx="http://www.nuxeo.org/server_management">
<workspace>
	<nx:server>
		<nx:productName>${This.runtime.getProperty("org.nuxeo.ecm.product.name")}</nx:productName>
		<nx:productVersion>${This.runtime.getProperty("org.nuxeo.ecm.product.version")}</nx:productVersion>
		<nx:environment>
			<nx:homedir>${This.environment.home}</nx:homedir>
			<nx:logdir>${This.environment.data}</nx:logdir>
			<nx:datadir>${This.environment.log}</nx:datadir>
			<nx:webdir>${This.environment.web}</nx:webdir>
			<nx:tmpdir>${This.environment.temp}</nx:tmpdir>
			<nx:appServer>${This.environment.isApplicationServer()?string}</nx:appServer>
			<nx:cmdLine>${This.environment.commandLineArgs}</nx:cmdLine>
		</nx:environment>
	</nx:server>
 	<collection href="${Context.URL}/bundles" nx:collectionType="bundles">
 		<title>Bundles</title>
 	</collection>
 	<collection href="${Context.URL}/components" nx:collectionType="components">
 		<title>Components</title>
 	</collection>
 	<collection href="${Context.URL}/resources" nx:collectionType="resources">
 		<title>Resources</title>
 	</collection>
</workspace>
</service>
 