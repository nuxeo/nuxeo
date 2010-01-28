package org.nuxeo.dam.platform.security;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.APPLICATION;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.webapp.security.GroupManagerActionsBean;

@Name("groupManagerActions")
@Scope(CONVERSATION)
@Install(precedence = APPLICATION)
public class DamGroupManagerActions extends GroupManagerActionsBean {

    private static final long serialVersionUID = 1L;
    
    protected String displayMode = BuiltinModes.VIEW;
    
    protected String selectedGroupId;

    public String getSelectedGroupId() {
        return selectedGroupId;
    }

    public void setSelectedGroupId(String selectedGroupId) throws ClientException {
        this.selectedGroupId = selectedGroupId;
        selectedGroup = userManager.getGroupModel(selectedGroupId);
    }

    public String deleteUser(String selectedGroupId) throws ClientException {
        if (selectedGroupId == null) {
            return super.deleteGroup();
        }
        
        try {
            userManager.deleteUser(selectedGroupId);

            resetGroups();
            return viewGroups();
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
    
    public List<String> getSubGroups(DocumentModel group) throws PropertyException, ClientException {
        List<String> result = new ArrayList<String>();
        
        Object subGroups = group.getPropertyValue("group:members");
        return result; 
    }
    
}
