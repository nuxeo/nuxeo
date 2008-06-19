import org.nuxeo.ecm.webengine.exceptions.*

def error = Context.getProperty("error");

if (error instanceof WebSecurityException) {
  Context.print("<h1>Error Page</h1>");
  Context.print("<h3>You don't have privileges to access this page</h3><hr/>");
  Context.print("You are currently running as <i>${Context.principal.name}</i>. To log-in under a different account please fill the following log-in form:");
  Context.print("<p><table width=\"100%\"><tr><td align=\"center\">");
  Context.render("/common/login.ftl");
  Context.print("</td></tr></table></p>");
} else if (error instanceof WebResourceNotFoundException) {
  Response.setStatus(404);
  Context.print("<h1>Error Page</h1>");
  Context.print("<h3>404 - Resource not found: ${Request.pathInfo}</h3><hr/>");
  error.printStackTrace(Response.writer);
} else {
  Response.setStatus(500);
  Context.print("<h1>Error Page</h1>");
  error.printStackTrace(Response.writer);
}

