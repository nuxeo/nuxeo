package org.nuxeo.dam.platform.security;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webapp.security.GroupManagerActionsBean;

@Name("groupManagerActions")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
public class DamGroupManagerActions extends GroupManagerActionsBean {

    private static final long serialVersionUID = 1L;

    protected String selectedGroupId;

    public String getSelectedGroupId() {
        return selectedGroupId;
    }

    public void setSelectedGroupId(String selectedGroupId)
            throws ClientException {
        this.selectedGroupId = selectedGroupId;
        selectedGroup = userManager.getGroupModel(selectedGroupId);
    }

    public void deleteGroupNoRedirect(String selectedGroupId) throws ClientException {
        if (selectedGroupId == null) {
            deleteGroup();
        }
        userManager.deleteGroup(selectedGroupId);
        resetGroups();
    }

    public void createGroupNoRedirect() throws ClientException {
        createGroup();
    }

    public void updateGroupNoRedirect() throws ClientException {
        updateGroup();
    }

}
