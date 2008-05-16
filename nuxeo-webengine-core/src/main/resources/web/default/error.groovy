import org.nuxeo.ecm.webengine.exceptions.*

error = Context.getProperty("error")
Context.print("<h1>Error Page</h1>");

if (error instanceof WebSecurityException) {
  Context.print("<h3>You don't have privileges to access this page</h3><hr/>");
}

error.printStackTrace(Response.getWriter())
