package org.nuxeo.dam.platform.security;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.webapp.security.UserManagerActionsBean;

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

    public String deleteUser(String selectedUserId) throws ClientException {
        if (selectedUserId == null) {
            return super.deleteUser();
        }
        
        try {
            userManager.deleteUser(selectedUserId);

            resetUsers();
            return viewUsers();
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
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

}
