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

    public static final String PROFILE_VIEW_MODE = "view";

    public static final String PROFILE_EDIT_MODE = "edit";

    public static final String PROFILE_EDIT_PASSWORD_MODE = "editPassword";

    @In(create = true)
    protected transient UserManagerActionsBean userManagerActions;

    @In(create = true)
    protected NuxeoPrincipal currentUser;

    protected String mode = PROFILE_VIEW_MODE;

    public void updateUser() throws ClientException {
        userManagerActions.updateUser();
        mode = PROFILE_VIEW_MODE;
    }

    public String getMode() throws ClientException {
        userManagerActions.setSelectedUser(currentUser.getModel());
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

}
