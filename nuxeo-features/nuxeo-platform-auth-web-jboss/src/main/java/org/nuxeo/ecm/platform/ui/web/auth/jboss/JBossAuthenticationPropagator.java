package org.nuxeo.ecm.platform.ui.web.auth.jboss;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.jboss.security.SecurityAssociation;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPropagator;

public class JBossAuthenticationPropagator implements
		NuxeoAuthenticationPropagator {


	public void propagateUserIdentificationInformation(
			CachableUserIdentificationInfo cachableUserIdent) {
		// JBoss specific implementation

        // need to transfer principal info onto calling thread...
        // this is normally done by ClientLoginModule, but in this
        // case we don't do a re-authentication.

        UserIdentificationInfo userInfo = cachableUserIdent.getUserInfo();

        final Object password = userInfo.getPassword().toCharArray();
        final Object cred = userInfo;
        final boolean useLP = userInfo.getLoginPluginName() != null;
        final Principal prin = cachableUserIdent.getPrincipal();
        final Subject subj = cachableUserIdent.getLoginContext().getSubject();

        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                if (useLP) {
                    SecurityAssociation.pushSubjectContext(subj, prin, cred);
                } else {
                    SecurityAssociation.pushSubjectContext(subj, prin, password);
                }
                return null;
            }
        });
	}

}
