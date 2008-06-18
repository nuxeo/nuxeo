import org.nuxeo.ecm.webengine.exceptions.*

def error = Context.getProperty("error");
Context.print("<h1>Error Page</h1>");

if (error instanceof WebSecurityException) {
  Context.print("<h3>You don't have privileges to access this page</h3><hr/>");
  Context.print("You are currently running as <i>${Context.principal.name}</i>. To log-in under a different account please fill the following log-in form:");
  Context.print("<p><table width=\"100%\"><tr><td align=\"center\">");
  Context.render("login.ftl");
  Context.print("</td></tr></table></p>");
} else {
  error.printStackTrace(Response.writer);
}

