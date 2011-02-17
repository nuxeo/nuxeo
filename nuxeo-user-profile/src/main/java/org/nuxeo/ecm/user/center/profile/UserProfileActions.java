package org.nuxeo.ecm.user.center.profile;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webapp.security.UserManagerActionsBean;

@Name("userProfileActions")
@Scope(ScopeType.CONVERSATION)
public class UserProfileActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient UserManagerActionsBean userManagerActions;

    @In(create = true)
    protected NuxeoPrincipal currentUser;

    protected boolean editing;

    public void updateUser() throws ClientException {
        userManagerActions.updateUser();
        editing = false;
    }

    public void changePassword() throws ClientException {
        userManagerActions.changePassword();
    }

    public boolean isEditing() throws ClientException {
        userManagerActions.setSelectedUser(currentUser.getModel());
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

}
