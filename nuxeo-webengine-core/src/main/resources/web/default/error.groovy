import org.nuxeo.ecm.webengine.exceptions.*
import java.io.*

// build args and stacktrace
def error = Context.getProperty("error")
def buf = new StringWriter()
def pw = new PrintWriter(buf)
error.printStackTrace(pw)
pw.close()
def stacktrace = buf.toString()
def args = ['error': error, 'stacktrace': stacktrace]

// locate template directory
def setErrorStatus = false // do not send error status when in client sub contexts  it may break ajax calls like for example for jquery tabs
def prefix
if (Context.clientContext != null) {
  prefix = "include/page"
} else {
  prefix = "include"
  setErrorStatus = true
}

// dispatch template rendering
if (error instanceof WebSecurityException) {
  if (setErrorStatus) { Response.setStatus(401) }
  Context.render("${prefix}/error_401.ftl", args)
} else if (error instanceof WebResourceNotFoundException) {
  if (setErrorStatus) { Response.setStatus(404) }
  Context.render("${prefix}/error_404.ftl", args)
} else {
  if (setErrorStatus) { Response.setStatus(500) }
  Context.render("${prefix}/error_500.ftl", args)
}

