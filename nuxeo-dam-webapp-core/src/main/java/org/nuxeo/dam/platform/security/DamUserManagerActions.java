package org.nuxeo.dam.platform.security;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.security.UserManagerActionsBean;

@Name("userManagerActions")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
public class DamUserManagerActions extends UserManagerActionsBean {

    private static final long serialVersionUID = 1L;

    public String deleteUser(DocumentModel userModel) throws ClientException {
        try {
            userManager.deleteUser(userModel);
            // refresh users and groups list
            resetUsers();
            return null;
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
    }

}
