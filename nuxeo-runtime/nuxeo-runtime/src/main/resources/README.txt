To enable servlet invoker for runtime services (i.e. running streaming and autoconf on HTTP)
you have to copy the servlet definition in the web.xml into a war web.xml file
Then in nuxeo.properties you should define the property

org.nuxeo.runtime.server.locator

that points to the remoting service locator. Example:

org.nuxeo.runtime.server.locator=servlet://localhost:8080/nuxeo/ServerInvokerServlet/?datatype=nuxeo

where http://localhost:8080/nuxeo/ServerInvokerServlet should be URL of the servlet invoker

On the Apogee side you create a connection using the Custom Platform wizard page
and copy there the previous URL:

 servlet://localhost:8080/nuxeo/ServerInvokerServlet/?datatype=nuxeo

