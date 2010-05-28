package org.nuxeo.apidoc.browse;

import org.nuxeo.apidoc.security.SecurityConstants;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebContext;

public class SecurityHelper {

    public static boolean canEditDocumentation(WebContext ctx) {
        NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
        return canEditDocumentation(principal);
    }

    public static boolean canEditDocumentation(NuxeoPrincipal principal) {

        if (principal.isAdministrator()) {
            return true;
        }
        if (principal.isAnonymous()) {
            return false;
        }
        return principal.getAllGroups().contains(SecurityConstants.Write_Group);

    }

}
