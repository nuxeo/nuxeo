package org.nuxeo.dam.platform.security;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.webapp.security.UserManagerActionsBean;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

@Name("userManagerActions")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
public class DamUserManagerActions extends UserManagerActionsBean {

    private static final long serialVersionUID = 1L;

    protected String displayMode = BuiltinModes.VIEW;

    protected String selectedUserId;

    public String getSelectedUserId() {
        return selectedUserId;
    }

    public void setSelectedUserId(String selectedUserId) throws ClientException {
        this.selectedUserId = selectedUserId;
        selectedUser = userManager.getUserModel(selectedUserId);
    }

    public void deleteUserNoRedirect(String selectedUserId) throws ClientException {
        if (selectedUserId == null) {
            deleteUser();
        }

        userManager.deleteUser(selectedUserId);
        resetUsers();
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void toggleDisplayMode() {
        if (BuiltinModes.VIEW.equals(displayMode)) {
            displayMode = BuiltinModes.EDIT;
        } else {
            displayMode = BuiltinModes.VIEW;
        }
    }

    public void createUserNoRedirect() throws ClientException {
        createUser();
    }

    public void updateUserNoRedirect() throws ClientException {
        updateUser();
    }

}
