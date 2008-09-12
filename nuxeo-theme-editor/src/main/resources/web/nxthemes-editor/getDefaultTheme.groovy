
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.NegotiationDef;
import org.nuxeo.theme.types.TypeRegistry;

defaultTheme = ""

typeRegistry = Manager.getTypeRegistry()
applicationPath = "/st"
application = typeRegistry.lookup(TypeFamily.APPLICATION, applicationPath);
 
if (application != null) {
    negotiation = application.getNegotiation();
    if (negotiation != null) {
        defaultTheme = negotiation.getDefaultTheme();
    }
}

return defaultTheme;
