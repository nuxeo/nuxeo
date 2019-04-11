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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.Labeler;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Holds the formatted security data, ready to be displayed. Holds the data on each map on the following structure: <br>
 * <p>
 * current doc grants: <br>
 * user 1 - perm1, perm2 <br>
 * user 2 - perm1, perm2 <br>
 * user 3 - perm2 <br>
 * current doc denies:<br>
 * ... <br>
 * parent doc grants:<br>
 * ... <br>
 * parent doc denies:<br>
 * ...<br>
 * <p>
 * Also has methods that allow manipulation of the contained data, such as add/remove security privileges. The end
 * result after add/remove has been called will be converted to a backend security DTO and then submitted on backend.
 *
 * @author Razvan Caraghin
 */
public class SecurityData implements Serializable {

    protected static final Labeler labeler = new Labeler("label.security.permission");

    private static final long serialVersionUID = -7958330304227141087L;

    private static final Log log = LogFactory.getLog(SecurityData.class);

    protected Map<String, List<String>> currentDocGrant = new HashMap<>();

    protected Map<String, List<String>> currentDocDeny = new HashMap<>();

    protected Map<String, List<String>> parentDocsGrant = new HashMap<>();

    protected Map<String, List<String>> parentDocsDeny = new HashMap<>();

    protected final List<String> currentDocumentUsers = new ArrayList<>();

    protected final List<String> parentDocumentsUsers = new ArrayList<>();

    protected String documentType = "";

    protected boolean needSave = false;

    public void setNeedSave(boolean needSave) {
        this.needSave = needSave;
    }

    public boolean getNeedSave() {
        return needSave;
    }

    public Map<String, List<String>> getCurrentDocDeny() {
        return currentDocDeny;
    }

    public void setCurrentDocDeny(Map<String, List<String>> deny) {
        currentDocDeny = deny;
    }

    public Map<String, List<String>> getCurrentDocGrant() {
        return currentDocGrant;
    }

    public void setCurrentDocGrant(Map<String, List<String>> grant) {
        currentDocGrant = grant;
    }

    public Map<String, List<String>> getParentDocsDenyLabels() {
        return buildLabelMap(parentDocsDeny);
    }

    /**
     * @since 6.0
     */
    public Map<String, List<String>> getCurrentDocDenyLabels() {
        return buildLabelMap(currentDocDeny);
    }

    public Map<String, List<String>> getParentDocsDeny() {
        return parentDocsDeny;
    }

    public void setParentDocsDeny(Map<String, List<String>> parentDocsDeny) {
        this.parentDocsDeny = parentDocsDeny;
    }

    public Map<String, List<String>> getParentDocsGrant() {
        return parentDocsGrant;
    }

    public Map<String, List<String>> getParentDocsGrantLabels() {
        return buildLabelMap(parentDocsGrant);
    }

    /**
     * @since 6.0
     */
    public Map<String, List<String>> getCurrentDocGrantLabels() {
        return buildLabelMap(currentDocGrant);
    }

    public void setParentDocsGrant(Map<String, List<String>> parentDocsGrant) {
        this.parentDocsGrant = parentDocsGrant;
    }

    public List<String> getCurrentDocumentUsers() {
        return currentDocumentUsers;
    }

    public List<String> getParentDocumentsUsers() {
        return parentDocumentsUsers;
    }

    protected Map<String, List<String>> buildLabelMap(Map<String, List<String>> permissions) {
        Map<String, List<String>> labelMap = new HashMap<>();

        for (String user : permissions.keySet()) {
            List<String> labels = new ArrayList<>();
            for (String perm : permissions.get(user)) {
                labels.add(labeler.makeLabel(perm));
            }
            labelMap.put(user, labels);
        }
        return labelMap;
    }

    public void rebuildUserLists() {
        List<String> users = new ArrayList<>();
        users.addAll(currentDocGrant.keySet());
        for (String user : currentDocDeny.keySet()) {
            if (!users.contains(user)) {
                users.add(user);
            }
        }

        currentDocumentUsers.clear();
        currentDocumentUsers.addAll(users);

        users.clear();
        users.addAll(parentDocsGrant.keySet());
        for (String user : parentDocsDeny.keySet()) {
            if (!users.contains(user)) {
                users.add(user);
            }
        }

        parentDocumentsUsers.clear();
        parentDocumentsUsers.addAll(users);
    }

    /**
     * Adds a privilege to the displayed list. This does not submit anything to the backend.
     */
    public void addModifiablePrivilege(String principalName, String permissionName, boolean grant) {
        if (null == principalName || null == permissionName) {
            log.error("Null params received, returning...");
            return;
        }

        needSave = true;
        if (grant) {
            // if we already have the user stored with rights we dont add the
            // user again, just update the list if needed
            if (null != currentDocGrant.get(principalName)) {
                // we already have the user - add the right to him
                boolean shouldAddPermission = true;
                for (String permission : currentDocGrant.get(principalName)) {
                    // only add the right to list if the right is not already
                    // there
                    if (permission.equals(permissionName)) {
                        shouldAddPermission = false;
                        break;
                    }
                }

                if (shouldAddPermission) {
                    currentDocGrant.get(principalName).add(permissionName);
                }
            } else {
                // add the user and create a new list of rights for him
                List<String> permissions = new ArrayList<>();
                permissions.add(permissionName);
                currentDocGrant.put(principalName, permissions);
            }
        } else {
            // if we already have the user stored with rights we dont add the
            // user again, just update the list if needed
            if (null != currentDocDeny.get(principalName)) {
                // we already have the user - add the right to him
                boolean shouldAddPermission = true;
                for (String permission : currentDocDeny.get(principalName)) {
                    // only add the right to list if the right is not already
                    // there
                    if (permission.equals(permissionName)) {
                        shouldAddPermission = false;
                        break;
                    }
                }

                if (shouldAddPermission) {
                    currentDocDeny.get(principalName).add(permissionName);
                }
            } else {
                // add the user and create a new list of rights for him
                List<String> permissions = new ArrayList<>();
                permissions.add(permissionName);
                currentDocDeny.put(principalName, permissions);
            }
        }

        rebuildUserLists();
    }

    /**
     * Removes a privilege from the displayed list. This does not submit anything to backend.
     *
     * @return true if a privilege was indeed removed
     */
    public boolean removeModifiablePrivilege(String principalName, String permissionName, boolean grant) {

        if (null == principalName || null == permissionName) {
            log.error("Null params received, returning...");
            return false;
        }

        needSave = true;
        boolean removed = false;
        if (grant) {
            if (null != currentDocGrant.get(principalName)) {
                // we have the specified user, check if we have the right
                Iterator<String> permissionIterator = currentDocGrant.get(principalName).iterator();
                while (permissionIterator.hasNext()) {
                    if (permissionIterator.next().equals(permissionName)) {
                        permissionIterator.remove();
                        removed = true;
                        break;
                    }
                }
            }
        } else {
            if (null != currentDocDeny.get(principalName)) {
                // we have the specified user, check if we have the right
                Iterator<String> permissionIterator = currentDocDeny.get(principalName).iterator();
                while (permissionIterator.hasNext()) {
                    if (permissionIterator.next().equals(permissionName)) {
                        permissionIterator.remove();
                        removed = true;
                        break;
                    }
                }
            }
        }

        rebuildUserLists();
        return removed;
    }

    /**
     * Removes all privileges for a given user. This does not edit the backend.
     */
    public void removeModifiablePrivilege(String principalName) {
        if (null == principalName) {
            log.error("Null principal received, returning...");
            return;
        }
        currentDocGrant.remove(principalName);

        // if user everyone had deny everything perm, then we have to
        // keep this perm
        if (principalName.equals(SecurityConstants.EVERYONE)) {

            final List<String> deniedPerms = currentDocDeny.get(principalName);
            boolean keepDenyAll = deniedPerms != null ? deniedPerms.contains(SecurityConstants.EVERYTHING) : false;

            currentDocDeny.remove(principalName);

            if (keepDenyAll) {
                addModifiablePrivilege(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
            }
        } else {
            currentDocDeny.remove(principalName);
        }

        needSave = true;
        rebuildUserLists();
    }

    /**
     * Adds an unmodifiable privilege to the displayed list (these are related to the parent documents).
     */
    public void addUnModifiablePrivilege(String principalName, String permissionName, boolean grant) {
        if (null == principalName || null == permissionName) {
            log.error("Null params received, returning...");
            return;
        }

        if (grant) {
            // if we already have the user stored with rights we dont add the
            // user again, just update the list if needed
            if (null != parentDocsGrant.get(principalName)) {
                // we already have the user - add the right to him
                boolean shouldAddPermission = true;
                for (String permission : parentDocsGrant.get(principalName)) {
                    // only add the right to list if the right is not already
                    // there
                    if (permission.equals(permissionName)) {
                        shouldAddPermission = false;
                        break;
                    }
                }

                if (shouldAddPermission) {
                    parentDocsGrant.get(principalName).add(permissionName);
                }
            } else {
                // add the user and create a new list of rights for him
                List<String> permissions = new ArrayList<>();
                permissions.add(permissionName);
                parentDocsGrant.put(principalName, permissions);
            }
        } else {
            // if we already have the user stored with rights we dont add the
            // user again, just update the list if needed
            if (null != parentDocsDeny.get(principalName)) {
                // we already have the user - add the right to him
                boolean shouldAddPermission = true;
                for (String permission : parentDocsDeny.get(principalName)) {
                    // only add the right to list if the right is not already
                    // there
                    if (permission.equals(permissionName)) {
                        shouldAddPermission = false;
                        break;
                    }
                }

                if (shouldAddPermission) {
                    parentDocsDeny.get(principalName).add(permissionName);
                }
            } else {
                // add the user and create a new list of rights for him
                List<String> permissions = new ArrayList<>();
                permissions.add(permissionName);
                parentDocsDeny.put(principalName, permissions);
            }

        }

        rebuildUserLists();
    }

    public void clear() {
        currentDocDeny.clear();
        currentDocGrant.clear();
        parentDocsDeny.clear();
        parentDocsGrant.clear();

        currentDocumentUsers.clear();
        parentDocumentsUsers.clear();

        log.debug("Cleared data...");
        needSave = false;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

}
