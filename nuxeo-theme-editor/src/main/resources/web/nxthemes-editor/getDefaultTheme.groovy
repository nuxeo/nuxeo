import org.nuxeo.theme.Manager
import org.nuxeo.theme.types.TypeFamily

defaultTheme = ""
applicationPath = Context.runScript("getApplicationPath.groovy")
application = Manager.getTypeRegistry().lookup(TypeFamily.APPLICATION, applicationPath)
 
if (application != null) {
    negotiation = application.getNegotiation()
    if (negotiation != null) {
        defaultTheme = negotiation.getDefaultTheme()
    }
}

return defaultTheme
