package org.nuxeo.ecm.user.registration.actions;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.user.registration.UserRegistrationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple UserRegistrationService Business Delegate
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 * @since 5.6
 */

@Name("org.nuxeo.ecm.user.registration.actions.UserRegistrationBusinessDelegate")
@Scope(ScopeType.APPLICATION)
public class UserRegistrationBusinessDelegate {
    @Factory(value = "userRegistrationService", scope = ScopeType.APPLICATION)
    public UserRegistrationService UserRegistrationServiceFactory() {
        return Framework.getService(UserRegistrationService.class);
    }
}
