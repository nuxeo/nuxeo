import org.nuxeo.ecm.webengine.exceptions.*
import java.io.*

// build args and stacktrace
def error = Context.getProperty("error")
def buf = new StringWriter()
def pw = new PrintWriter(buf)
error.printStackTrace(pw)
pw.close()
def stacktrace = buf.toString()
def args = ['error' : error, 'stacktrace' : stacktrace]

// locate template directory
def prefix
if (Context.getClientContext() != null) {
  prefix = "include/page"
} else {
  prefix = "include"
}

// dispatch template rendering
if (error instanceof WebSecurityException) {
  Response.setStatus(401)
  Context.render("${prefix}/error_401.ftl", args)
} else if (error instanceof WebResourceNotFoundException) {
  Response.setStatus(404)
  Context.render("${prefix}/error_404.ftl", args)
} else {
  Response.setStatus(500);
  Context.render("${prefix}/error_500.ftl", args)
}
