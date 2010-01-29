/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     eugen
 */
package org.nuxeo.dam.platform.action;

import static org.jboss.seam.ScopeType.PAGE;
import static org.jboss.seam.annotations.Install.APPLICATION;
import static org.nuxeo.dam.platform.context.ImportActionsBean.IMPORTSET_ROOT_PATH;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.event.script.FakeCompiledScript;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.runtime.api.Framework;

/**
 * @author eugen
 *
 */

@Name("folderAdminActions")
@Scope(ScopeType.PAGE)
@Install(precedence = APPLICATION)
public class DamFolderAdminActionBean implements Serializable{

    // TODO change this to Workspace after merge 
    private static final String FOLDER_TYPE = "Folder";

    private static final String VIEW_FOLDERS = "view_folders";

    private static final long serialVersionUID = 1L;

    public static final DocumentModelList NO_FOLDERS = new DocumentModelListImpl();

    @In(create = true)
    protected transient UserManager userManager;

    @DataModel(value="folderList")
    protected DocumentModelList folders;

    @DataModelSelection("folderList")
    protected DocumentModel selectedFolder;

    protected DocumentModel newFolder;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected String displayMode = BuiltinModes.VIEW;

    Map<String, List<String>> userPermissions = new HashMap<String, List<String>>();

    Map<String, List<String>> groupPermissions = new HashMap<String, List<String>>();

    @DataModel(value="visiblePermissions")
    List<String> visiblePermissions;

    @Factory(value = "folderList")
    public DocumentModelList getFolders() throws ClientException {
        if ( folders == null) {
            folders = queryModelActions.get("IMPORT_FOLDERS").getDocuments(documentManager);
        }
        if ( folders == null ) { // still null
            folders = NO_FOLDERS;
        }
        return folders;
    }

    public String deleteFolder(String folderId) throws ClientException {
        documentManager.removeDocument(new IdRef(folderId));
        documentManager.save();
        resetFolderList();
        return VIEW_FOLDERS;
    }

    public void resetFolderList() {
        folders = null;
    }
    
    public void resetNewFolder() {
        newFolder = null;
        userPermissions.clear();
        groupPermissions.clear();
        displayMode = BuiltinModes.EDIT;
    }


    public void toggleDisplayMode() {
        if (BuiltinModes.VIEW.equals(displayMode)) {
            displayMode = BuiltinModes.EDIT;
        } else {
            displayMode = BuiltinModes.VIEW;
        }
    }

    public String createFolder() throws ClientException {
        try {
            DocumentModel doc = documentManager.createDocument(newFolder);
            savePermissionMaps(doc);
            documentManager.save();
            resetNewFolder();
            resetFolderList();
            return VIEW_FOLDERS;
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
    }

    public String updateFolder() throws ClientException {
        try {
            selectedFolder = documentManager.saveDocument(selectedFolder);
            savePermissionMaps(selectedFolder);
            documentManager.save();
            resetFolderList();
            return VIEW_FOLDERS;
        } catch (Exception t) {
            throw ClientException.wrap(t);
        }
    }

    public DocumentModel getNewFolder() throws ClientException {
        if ( newFolder == null ) {
            newFolder = documentManager.createDocumentModel(IMPORTSET_ROOT_PATH, IdUtils.generateStringId(), FOLDER_TYPE);
        }
        return newFolder;
    }

    public void setSelectedFolder(String id) throws ClientException{
        selectedFolder = documentManager.getDocument(new IdRef(id));
        loadPermissionMaps();
        displayMode = BuiltinModes.VIEW;
    }

    private void savePermissionMaps(DocumentModel doc) throws ClientException {
        try {
            ACP acp = doc.getACP();
            ACL acl = acp.getACL(ACL.LOCAL_ACL);
            if ( acl == null ) {
                acl = new ACLImpl();
                acp.addACL(acl);
            } else {
                acl.clear();
            }
            savePermissionMap(userPermissions, acl);
            savePermissionMap(groupPermissions, acl);
            documentManager.setACP(doc.getRef(), acp , true);
        } catch ( Throwable t ){
            throw ClientException.wrap(t);
        }
        
    }

    private void savePermissionMap(Map<String, List<String>> map, ACL acl) {
        for ( Entry<String, List<String>> entry : map.entrySet()){
            String perm = entry.getKey();
            List<String> list = entry.getValue();
            for ( String id : list){
                acl.add(new ACE(id, perm, true));
            }
        }
    }

    
    
    private void loadPermissionMaps() throws ClientException {
        groupPermissions.clear();
        userPermissions.clear();
        try {
            ACP acp = selectedFolder.getACP();
            ACL acl = acp.getACL(ACL.LOCAL_ACL);
            if ( acl != null ) {
                ACE[] aces = acl.getACEs();
                for ( ACE ace : aces) {
                    if ( ace.isGranted() ){
                        String perm = ace.getPermission();
                        if( visiblePermissions.contains(perm)){
                            String id = ace.getUsername();
                            if ( userManager.getGroup(id) != null){ // group
                                loadPermission(groupPermissions, perm, id);
                            } else { // user
                                loadPermission(userPermissions, perm, id);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }
    
    
    private void loadPermission(Map<String, List<String>> map, String perm, String id) {
        List<String> list = map.get(perm);
        if( list == null) {
            list = new ArrayList<String>();
            map.put(perm, list);
        }
        list.add(id);
    }

    @Factory(value = "visiblePermissions")
    public List<String> getVisiblePermissions() throws ClientException{
        try {
            if ( visiblePermissions == null ) {
                visiblePermissions = new ArrayList<String>();
                SecurityService securityService = Framework.getService(SecurityService.class);
                List<UserVisiblePermission> permDescriptors = securityService.getPermissionProvider().getUserVisiblePermissionDescriptors("Workspace");
                for ( UserVisiblePermission pd : permDescriptors) {
                    visiblePermissions.add(pd.getPermission());
                }
            }
            return visiblePermissions;
        } catch ( Throwable t ) {
            throw ClientException.wrap(t);
        }
    }

    public Map<String, List<String>> getUserPermissions() {
        return userPermissions;
    }

    public Map<String, List<String>> getGroupPermissions() {
        return groupPermissions;
    }

    public DocumentModel getSelectedFolder() {
        return selectedFolder;
    }

    public String getDisplayMode() {
        return displayMode;
    }

}
