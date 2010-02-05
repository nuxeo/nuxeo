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
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.runtime.api.Framework;

import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.dam.platform.context.ImportActions.IMPORTSET_ROOT_PATH;

/**
 * @author eugen
 *
 */
@Name("folderAdminActions")
@Scope(ScopeType.PAGE)
@Install(precedence = FRAMEWORK)
public class DamFolderAdminActions implements Serializable {

    protected static final String FOLDER_TYPE = "Workspace";

    protected static final String VIEW_FOLDERS = "view_folders";

    protected static final long serialVersionUID = 1L;

    public static final DocumentModelList NO_FOLDERS = new DocumentModelListImpl();

    @In(create = true)
    protected transient UserManager userManager;

    protected DocumentModelList folders;

    protected DocumentModel selectedFolder;

    protected DocumentModel newFolder;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected String displayMode = BuiltinModes.VIEW;

    protected Map<String, List<String>> userPermissions = new HashMap<String, List<String>>();

    protected Map<String, List<String>> groupPermissions = new HashMap<String, List<String>>();

    protected List<String> visiblePermissions;

    @Factory(value = "folderList")
    public DocumentModelList getFolders() throws ClientException {
        if (folders == null) {
            folders = queryModelActions.get("IMPORT_FOLDERS").getDocuments(
                    documentManager);
        }
        if (folders == null) { // still null
            folders = NO_FOLDERS;
        }
        return folders;
    }

    public void deleteFolderNoRedirect(String folderId) throws ClientException {
        documentManager.removeDocument(new IdRef(folderId));
        documentManager.save();
        resetFolderList();
    }

    public void resetFolderList() {
        folders = null;
    }

    public void resetNewFolder() {
        newFolder = null;
        resetPermissions();
        displayMode = BuiltinModes.EDIT;
    }

    public void resetPermissions() {
        userPermissions.clear();
        groupPermissions.clear();
        for (String perm : visiblePermissions) {
            userPermissions.put(perm, new ArrayList<String>());
            groupPermissions.put(perm, new ArrayList<String>());
        }
    }

    public void toggleDisplayMode() {
        if (BuiltinModes.VIEW.equals(displayMode)) {
            displayMode = BuiltinModes.EDIT;
        } else {
            displayMode = BuiltinModes.VIEW;
        }
    }

    public void createFolderNoRedirect() throws ClientException {
        DocumentModel doc = documentManager.createDocument(newFolder);
        savePermissionMaps(doc);
        documentManager.save();
        resetNewFolder();
        resetFolderList();
    }

    public void updateFolderNoRedirect() throws ClientException {
        selectedFolder = documentManager.saveDocument(selectedFolder);
        savePermissionMaps(selectedFolder);
        documentManager.save();
        resetFolderList();
    }

    public DocumentModel getNewFolder() throws ClientException {
        if (newFolder == null) {
            newFolder = documentManager.createDocumentModel(
                    IMPORTSET_ROOT_PATH, IdUtils.generateStringId(),
                    FOLDER_TYPE);
        }
        return newFolder;
    }

    public void setSelectedFolder(String id) throws ClientException {
        selectedFolder = documentManager.getDocument(new IdRef(id));
        loadPermissionMaps();
        displayMode = BuiltinModes.VIEW;
    }

    protected void savePermissionMaps(DocumentModel doc) throws ClientException {
        ACP acp = doc.getACP();
        ACL acl = acp.getACL(ACL.LOCAL_ACL);
        if (acl == null) {
            acl = new ACLImpl();
            acp.addACL(acl);
        } else {
            acl.clear();
        }
        savePermissionMap(userPermissions, acl);
        savePermissionMap(groupPermissions, acl);
        documentManager.setACP(doc.getRef(), acp, true);
    }

    protected void savePermissionMap(Map<String, List<String>> map, ACL acl) {
        for (Entry<String, List<String>> entry : map.entrySet()) {
            String perm = entry.getKey();
            List<String> list = entry.getValue();
            for (String id : list) {
                acl.add(new ACE(id, perm, true));
            }
        }
    }

    protected void loadPermissionMaps() throws ClientException {
        groupPermissions.clear();
        userPermissions.clear();
        ACP acp = selectedFolder.getACP();
        ACL acl = acp.getACL(ACL.LOCAL_ACL);
        if (acl != null) {
            ACE[] aces = acl.getACEs();
            for (ACE ace : aces) {
                if (ace.isGranted()) {
                    String perm = ace.getPermission();
                    if (visiblePermissions.contains(perm)) {
                        String id = ace.getUsername();
                        if (userManager.getGroup(id) != null) { // group
                            loadPermission(groupPermissions, perm, id);
                        } else { // user
                            loadPermission(userPermissions, perm, id);
                        }
                    }
                }
            }
        }
    }

    protected void loadPermission(Map<String, List<String>> map, String perm,
            String id) {
        List<String> list = map.get(perm);
        if (list == null) {
            list = new ArrayList<String>();
            map.put(perm, list);
        }
        list.add(id);
    }

    @Factory(value = "visiblePermissions")
    public List<String> getVisiblePermissions() throws ClientException {
        try {
            if (visiblePermissions == null) {
                visiblePermissions = new ArrayList<String>();
                SecurityService securityService = Framework.getService(SecurityService.class);
                List<UserVisiblePermission> permDescriptors = securityService.getPermissionProvider().getUserVisiblePermissionDescriptors(
                        FOLDER_TYPE);
                for (UserVisiblePermission pd : permDescriptors) {
                    visiblePermissions.add(pd.getPermission());
                }
            }
            return visiblePermissions;
        } catch (Exception e) {
            throw ClientException.wrap(e);
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
