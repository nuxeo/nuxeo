/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.utils.UserAgentMatcher;
import org.nuxeo.common.utils.i18n.Labeler;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.UserEntry;
import org.nuxeo.ecm.core.api.security.UserVisiblePermission;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.query.api.PageSelection;
import org.nuxeo.ecm.platform.query.api.PageSelections;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides security related methods.
 *
 * @author Razvan Caraghin
 */
@Name("securityActions")
@Scope(CONVERSATION)
public class SecurityActionsBean extends InputController implements SecurityActions, Serializable {

    private static final long serialVersionUID = -7190826911734958662L;

    private static final Log log = LogFactory.getLog(SecurityActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected PermissionActionListManager permissionActionListManager;

    @In(create = true)
    protected PermissionListManager permissionListManager;

    @In(create = true)
    protected PrincipalListManager principalListManager;

    @In(create = true)
    protected transient UserManager userManager;

    @In(create = true)
    protected NuxeoPrincipal currentUser;

    protected static final String[] SEED_PERMISSIONS_TO_CHECK = { SecurityConstants.WRITE_SECURITY,
            SecurityConstants.READ_SECURITY };

    private static final Labeler labeler = new Labeler("label.security.permission");

    protected String[] CACHED_PERMISSION_TO_CHECK;

    protected SecurityData securityData;

    protected boolean obsoleteSecurityData = true;

    protected PageSelections<String> entries;

    protected transient List<String> cachedValidatedUserAndGroups;

    protected transient List<String> cachedDeletedUserAndGroups;

    private Boolean blockRightInheritance;

    protected String selectedEntry;

    protected List<String> selectedEntries;

    @Override
    @Observer(value = EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED, create = false)
    @BypassInterceptors
    public void resetSecurityData() {
        obsoleteSecurityData = true;
        blockRightInheritance = null;
    }

    @Override
    public void rebuildSecurityData() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            if (securityData == null) {
                securityData = new SecurityData();
                securityData.setDocumentType(currentDocument.getType());
            }
            ACP acp = documentManager.getACP(currentDocument.getRef());

            if (acp != null) {
                SecurityDataConverter.convertToSecurityData(acp, securityData);
            } else {
                securityData.clear();
            }

            reconstructTableModel();

            // Check if the inherited rights are activated
            List<String> deniedPerms = securityData.getCurrentDocDeny().get(SecurityConstants.EVERYONE);
            if (deniedPerms != null && deniedPerms.contains(SecurityConstants.EVERYTHING)) {
                blockRightInheritance = Boolean.TRUE;
            }

            if (blockRightInheritance == null) {
                blockRightInheritance = Boolean.FALSE;
            }
            obsoleteSecurityData = false;
        }
    }

    /**
     * Update the dataTableModel from the current {@link SecurityData} this method is automatically called by
     * rebuildSecurityData method
     */
    protected void reconstructTableModel() {
        List<String> items = getCurrentDocumentUsers();
        entries = new PageSelections<String>();
        if (items != null) {
            for (String item : items) {
                if (SecurityConstants.EVERYONE.equals(item)) {
                    final List<String> grantedPerms = securityData.getCurrentDocGrant().get(item);
                    final List<String> deniedPerms = securityData.getCurrentDocDeny().get(item);
                    if (deniedPerms != null && deniedPerms.contains(SecurityConstants.EVERYTHING)
                            && grantedPerms == null && deniedPerms.size() == 1) {
                        // the only perm is deny everything, there is no need to display the row
                        continue;
                    }
                }
                entries.add(new PageSelection<String>(item, false));
            }
        }
    }

    @Override
    public PageSelections<String> getDataTableModel() {
        if (obsoleteSecurityData) {
            // lazy initialization at first time access
            rebuildSecurityData();
        }

        return entries;
    }

    @Override
    public SecurityData getSecurityData() {
        if (obsoleteSecurityData) {
            // lazy initialization at first time access
            rebuildSecurityData();
        }
        return securityData;
    }

    @Override
    public String updateSecurityOnDocument() {
        List<UserEntry> modifiableEntries = SecurityDataConverter.convertToUserEntries(securityData);
        ACP acp = currentDocument.getACP();

        if (acp == null) {
            acp = new ACPImpl();
        }

        acp.setRules(modifiableEntries.toArray(new UserEntry[0]));

        currentDocument.setACP(acp, true);
        documentManager.save();
        Events.instance().raiseEvent(EventNames.DOCUMENT_SECURITY_CHANGED);

        // Reread data from the backend to be sure the current bean
        // state is uptodate w.r.t. the real backend state
        rebuildSecurityData();

        // Type currentType = typeManager.getType(currentDocument.getType());
        // return applicationController.getPageOnEditedDocumentType(currentType);

        // Forward to default view, that's not what we want
        // return navigationContext.getActionResult(currentDocument, UserAction.AFTER_EDIT);

        // Temporary fix, to avoid forward to default_view.
        // The same page is reloaded after submit.
        // May use UserAction, with new kind of action (AFTER_EDIT_RIGHTS)?
        return null;
    }

    @Override
    public String addPermission(String principalName, String permissionName, boolean grant) {
        if (securityData == null) {
            securityData = getSecurityData();
        }

        String grantPerm = permissionName;
        String denyPerm = permissionName;
        List<UserVisiblePermission> uvps = getVisibleUserPermissions(securityData.getDocumentType());
        if (uvps != null) {
            for (UserVisiblePermission uvp : uvps) {
                if (uvp.getId().equals(permissionName)) {
                    grantPerm = uvp.getPermission();
                    denyPerm = uvp.getDenyPermission();
                    break;
                }
            }
        } else {
            log.debug("no entry for documentType in visibleUserPermissions this should never happend, using default mapping ...");
        }

        if (grant) {
            // remove the opposite rule if any
            boolean removed = securityData.removeModifiablePrivilege(principalName, denyPerm, !grant);
            if (!removed) {
                removed = securityData.removeModifiablePrivilege(principalName, grantPerm, !grant);
            }
            // add rule only if none was removed
            if (!removed) {
                securityData.addModifiablePrivilege(principalName, grantPerm, grant);
            }
        } else {
            // remove the opposite rule if any
            boolean removed = securityData.removeModifiablePrivilege(principalName, grantPerm, !grant);
            if (!removed) {
                removed = securityData.removeModifiablePrivilege(principalName, denyPerm, !grant);
            }
            // add rule only if none was removed
            if (!removed) {
                securityData.addModifiablePrivilege(principalName, denyPerm, grant);
            }
        }
        reconstructTableModel();
        return null;
    }

    @Override
    public String addPermission() {
        String permissionName = permissionListManager.getSelectedPermission();
        boolean grant = permissionActionListManager.getSelectedGrant().equals("Grant");
        return addPermission(selectedEntry, permissionName, grant);
    }

    @Override
    public String addPermissions() {
        if (selectedEntries == null || selectedEntries.isEmpty()) {
            String message = ComponentUtils.translate(FacesContext.getCurrentInstance(),
                    "error.rightsManager.noUsersSelected");
            FacesMessages.instance().add(message);
            return null;
        }
        String permissionName = permissionListManager.getSelectedPermission();
        boolean grant = permissionActionListManager.getSelectedGrant().equals("Grant");

        for (String principalName : selectedEntries) {
            addPermission(principalName, permissionName, grant);
        }
        return null;
    }

    @Override
    public String addPermissionAndUpdate() {
        addPermission();
        updateSecurityOnDocument();
        return null;
    }

    @Override
    public String addPermissionsAndUpdate() {
        addPermissions();
        updateSecurityOnDocument();
        selectedEntries = null;
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get("message.updated.rights"));
        return null;
    }

    @Override
    public String saveSecurityUpdates() {
        updateSecurityOnDocument();
        selectedEntries = null;
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get("message.updated.rights"));
        return null;
    }

    @Override
    public String removePermission() {
        securityData.removeModifiablePrivilege(selectedEntry, permissionListManager.getSelectedPermission(),
                permissionActionListManager.getSelectedGrant().equals("Grant"));
        reconstructTableModel();
        return null;
    }

    @Override
    public String removePermissionAndUpdate() {
        removePermission();

        if (!checkPermissions()) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get("message.updated.rights"));
            return null;
        }

        updateSecurityOnDocument();
        // do not redirect to the default folder view
        return null;
    }

    @Override
    public String removePermissions() {
        for (PageSelection<String> user : getSelectedRows()) {
            securityData.removeModifiablePrivilege(user.getData());
            if (!checkPermissions()) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get("message.error.removeRight"));
                return null;
            }
        }
        reconstructTableModel();
        return null;
    }

    @Override
    public String removePermissionsAndUpdate() {
        for (PageSelection<String> user : getDataTableModel().getEntries()) {
            securityData.removeModifiablePrivilege(user.getData());
            if (!checkPermissions()) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get("message.error.removeRight"));
                return null;
            }
        }
        updateSecurityOnDocument();
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get("message.updated.rights"));
        // do not redirect to the default folder view
        return null;
    }

    @Override
    public boolean getCanAddSecurityRules() {
        return documentManager.hasPermission(currentDocument.getRef(), "WriteSecurity");
    }

    @Override
    public boolean getCanRemoveSecurityRules() {
        return documentManager.hasPermission(currentDocument.getRef(), "WriteSecurity") && !getSelectedRows().isEmpty();
    }

    /**
     * @return The list of selected rows in the local rights table.
     * @since 6.0
     */
    private List<PageSelection<String>> getSelectedRows() {
        List<PageSelection<String>> selectedRows = new ArrayList<PageSelection<String>>();

        if (!getDataTableModel().isEmpty()) {
            for (PageSelection<String> entry : getDataTableModel().getEntries()) {
                if (entry.isSelected()) {
                    selectedRows.add(entry);
                }
            }
        }
        return selectedRows;
    }

    public List<UserVisiblePermission> getVisibleUserPermissions(String documentType) {
        return Framework.getService(PermissionProvider.class).getUserVisiblePermissionDescriptors(documentType);
    }

    @Override
    public List<SelectItem> getSettablePermissions() {
        String documentType = navigationContext.getCurrentDocument().getType();

        // BBB: use the platform service if it defines permissions (deprecated)
        UIPermissionService service = (UIPermissionService) Framework.getRuntime().getComponent(
                UIPermissionService.NAME);
        String[] settablePermissions = service.getUIPermissions(documentType);

        if (settablePermissions == null || settablePermissions.length == 0) {
            // new centralized permission provider at the core level

            List<UserVisiblePermission> visiblePerms = getVisibleUserPermissions(documentType);
            settablePermissions = new String[visiblePerms.size()];
            int idx = 0;
            for (UserVisiblePermission uvp : visiblePerms) {
                settablePermissions[idx] = uvp.getId();
                idx++;
            }
        }

        return asSelectItems(settablePermissions);
    }

    protected List<SelectItem> asSelectItems(String... permissions) {
        List<SelectItem> items = new ArrayList<SelectItem>();
        for (String perm : permissions) {
            String label = labeler.makeLabel(perm);
            SelectItem it = new SelectItem(perm, resourcesAccessor.getMessages().get(label));
            items.add(it);
        }
        return items;
    }

    /**
     * @since 7.4
     */
    public List<SelectItem> getUserVisiblePermissionSelectItems(String documentType) {
        List<UserVisiblePermission> userVisiblePermissions = getVisibleUserPermissions(documentType);
        List<String> permissions = new ArrayList<>();
        for (UserVisiblePermission userVisiblePermission : userVisiblePermissions) {
            permissions.add(userVisiblePermission.getId());
        }
        return asSelectItems(permissions.toArray(new String[permissions.size()]));
    }

    @Override
    public Map<String, String> getIconAltMap() {
        return principalListManager.iconAlt;
    }

    @Override
    public Map<String, String> getIconPathMap() {
        return principalListManager.iconPath;
    }

    @Override
    public Boolean getBlockRightInheritance() {
        return blockRightInheritance;
    }

    @Override
    public void setBlockRightInheritance(Boolean blockRightInheritance) {
        this.blockRightInheritance = blockRightInheritance;
    }

    public String blockRightInheritance() {
        Boolean needBlockRightInheritance = blockRightInheritance;

        if (needBlockRightInheritance) {
            // Block
            securityData.addModifiablePrivilege(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
            // add user to avoid lock up
            Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
            if (securityData.getCurrentDocumentUsers() != null
                    && !securityData.getCurrentDocumentUsers().contains(currentUser.getName())) {
                securityData.addModifiablePrivilege(currentUser.getName(), SecurityConstants.EVERYTHING, true);
                // add administrators to avoid LockUp
                List<String> adminGroups = userManager.getAdministratorsGroups();
                for (String adminGroup : adminGroups) {
                    securityData.addModifiablePrivilege(adminGroup, SecurityConstants.EVERYTHING, true);
                }
            }
        } else {
            securityData.removeModifiablePrivilege(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
        }
        updateSecurityOnDocument();
        selectedEntries = null;
        return null;
    }

    @Override
    public Boolean displayInheritedPermissions() {
        return getDisplayInheritedPermissions();
    }

    @Override
    public boolean getDisplayInheritedPermissions() {
        if (blockRightInheritance == null) {
            rebuildSecurityData();
        }
        if (blockRightInheritance) {
            return false;
        }
        return !securityData.getParentDocumentsUsers().isEmpty();
    }

    @Override
    public List<String> getCurrentDocumentUsers() {
        List<String> currentUsers = securityData.getCurrentDocumentUsers();
        return validateUserGroupList(currentUsers);
    }

    @Override
    public List<String> getParentDocumentsUsers() {
        List<String> parentUsers = securityData.getParentDocumentsUsers();
        return validateUserGroupList(parentUsers);
    }

    /**
     * Validates user/group against userManager in order to remove obsolete entries (ie: deleted groups/users).
     */
    private List<String> validateUserGroupList(List<String> usersGroups2Validate) {
        // TODO :
        // 1 -should add a clean cache system to avoid
        // calling the directory : this can be problematic for big ldaps
        // 2 - this filtering should at some point be applied to acp and saved
        // back in a batch?

        List<String> returnList = new ArrayList<String>();
        for (String entry : usersGroups2Validate) {
            if (entry.equals(SecurityConstants.EVERYONE)) {
                returnList.add(entry);
                continue;
            }
            if (isUserGroupInCache(entry)) {
                returnList.add(entry);
                continue;
            }
            if (isUserGroupInDeletedCache(entry)) {
                continue;
            }

            if (userManager.getPrincipal(entry) != null) {
                returnList.add(entry);
                addUserGroupInCache(entry);
                continue;
            } else if (userManager.getGroup(entry) != null) {
                returnList.add(entry);
                addUserGroupInCache(entry);
                continue;
            } else {
                addUserGroupInDeletedCache(entry);
            }
        }
        return returnList;
    }

    private Boolean isUserGroupInCache(String entry) {
        if (cachedValidatedUserAndGroups == null) {
            return false;
        }
        return cachedValidatedUserAndGroups.contains(entry);
    }

    private void addUserGroupInCache(String entry) {
        if (cachedValidatedUserAndGroups == null) {
            cachedValidatedUserAndGroups = new ArrayList<String>();
        }
        cachedValidatedUserAndGroups.add(entry);
    }

    private Boolean isUserGroupInDeletedCache(String entry) {
        if (cachedDeletedUserAndGroups == null) {
            return false;
        }
        return cachedDeletedUserAndGroups.contains(entry);
    }

    private void addUserGroupInDeletedCache(String entry) {
        if (cachedDeletedUserAndGroups == null) {
            cachedDeletedUserAndGroups = new ArrayList<String>();
        }

        cachedDeletedUserAndGroups.add(entry);
    }

    /**
     * Checks if the current user can still read and write access rights. If he can't, then the security data are
     * rebuilt.
     */
    private boolean checkPermissions() {
        if (currentUser.isAdministrator()) {
            return true;
        } else {
            List<String> principals = new ArrayList<String>();
            principals.add(currentUser.getName());
            principals.addAll(currentUser.getAllGroups());

            ACP acp = currentDocument.getACP();
            new SecurityDataConverter();
            List<UserEntry> modifiableEntries = SecurityDataConverter.convertToUserEntries(securityData);

            if (null == acp) {
                acp = new ACPImpl();
            }
            acp.setRules(modifiableEntries.toArray(new UserEntry[0]));

            final boolean access = acp.getAccess(principals.toArray(new String[0]), getPermissionsToCheck())
                                      .toBoolean();
            if (!access) {
                rebuildSecurityData();
            }
            return access;
        }
    }

    protected String[] getPermissionsToCheck() {
        if (CACHED_PERMISSION_TO_CHECK == null) {
            PermissionProvider pprovider = Framework.getService(PermissionProvider.class);
            List<String> aggregatedPerms = new LinkedList<String>();
            for (String seedPerm : SEED_PERMISSIONS_TO_CHECK) {
                aggregatedPerms.add(seedPerm);
                String[] compoundPerms = pprovider.getPermissionGroups(seedPerm);
                if (compoundPerms != null) {
                    aggregatedPerms.addAll(Arrays.asList(compoundPerms));
                }
            }
            CACHED_PERMISSION_TO_CHECK = aggregatedPerms.toArray(new String[aggregatedPerms.size()]);
        }
        return CACHED_PERMISSION_TO_CHECK;
    }

    @Override
    public String getSelectedEntry() {
        return selectedEntry;
    }

    @Override
    public void setSelectedEntry(String selectedEntry) {
        this.selectedEntry = selectedEntry;
    }

    @Override
    public List<String> getSelectedEntries() {
        return selectedEntries;
    }

    @Override
    public void setSelectedEntries(List<String> selectedEntries) {
        this.selectedEntries = selectedEntries;
    }

    @Factory(value = "isMSIEorEdge", scope = ScopeType.SESSION)
    public boolean isMSIEorEdge() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            String ua = request.getHeader("User-Agent");
            return UserAgentMatcher.isMSIE6or7(ua) || UserAgentMatcher.isMSIE10OrMore(ua)
                    || UserAgentMatcher.isMSEdge(ua);
        } else {
            return false;
        }
    }

    public String getLabel(String permission) {
        return StringUtils.isNotBlank(permission) ? labeler.makeLabel(permission) : permission;
    }

    /**
     * Returns a Map containing all contributed permissions and their associated labels.
     *
     * @since 8.1
     */
    public Map<String, String> getPermissionsToLabels() {
        PermissionProvider permissionProvider = Framework.getService(PermissionProvider.class);
        String[] permissions = permissionProvider.getPermissions();
        Map<String, String> permissionsToLabels = new HashMap<>();
        for (String permission : permissions) {
            permissionsToLabels.put(permission, getLabel(permission));
        }
        return permissionsToLabels;
    }

}
