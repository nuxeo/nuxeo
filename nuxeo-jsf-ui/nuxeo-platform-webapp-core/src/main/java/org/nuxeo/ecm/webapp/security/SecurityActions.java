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

import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.nuxeo.ecm.platform.query.api.PageSelections;

/**
 * Provides security related operations on the current document.
 *
 * @author Razvan Caraghin
 */
public interface SecurityActions {

    /**
     * Submits the security changes to the backend.
     *
     * @return the page that will be displayed next
     */
    String updateSecurityOnDocument();

    /**
     * Adds a permission to the list of permissions for the current document. After all client side changes are made,
     * then the list of permissions need to be submitted on backend using <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     */
    String addPermission();

    /**
     * Adds a list of permission to the list of permissions for the current document. After all client side changes are
     * made, then the list of permissions need to be submitted on backend using <code>updateSecurityOnDocument()></code>
     * .
     *
     * @return the page that needs to be displayed next
     */
    String addPermissions();

    String addPermission(String principalName, String permissionName, boolean grant);

    /**
     * Removes a permission from the list of permissions for the current document. After all client side changes are
     * made, then the list of permissions need to be submitted on backend using <code>updateSecurityOnDocument()></code>
     * .
     *
     * @return the page that needs to be displayed next
     */
    String removePermission();

    /**
     * Adds a permission to the list of permissions for the current document and automatically update the backend with
     * <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     */
    String addPermissionAndUpdate();

    /**
     * Adds a list of permissions to the list of permissions for the current document and automatically update the
     * backend with <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     */
    String addPermissionsAndUpdate();

    /**
     * Removes a permission from the list of permissions for the current document and automatically update the backend
     * with <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     */
    String removePermissionAndUpdate();

    String removePermissionsAndUpdate();

    /**
     * Marks the current security data info as obsolete so that it gets lazily recomputed from the backend the next time
     * it is accessed.
     */
    void resetSecurityData();

    /**
     * Rebuilds the security displayable data from the current selected document.
     */
    void rebuildSecurityData();

    /**
     * @return a PageSelections used to build a checkboxable listing of managed permissions
     */
    PageSelections<String> getDataTableModel();

    /**
     * @return the SecurityData object that manages a stateful representation of the permissions mapping that apply to
     *         the current document (inherited or not)
     */
    SecurityData getSecurityData();

    /**
     * Returns true if the implementator if the principal has the permission to add new security rules on currentItem.
     */
    boolean getCanAddSecurityRules();

    /**
     * Returns true if the implementator can provide a list of permissions delete now and the principal has
     * WriteSecurity permission on the currentItem.
     */
    boolean getCanRemoveSecurityRules();

    /**
     * @return the list of permissions the users can set through the rights management tab
     */
    List<SelectItem> getSettablePermissions();

    /**
     * Maps the principal type to the icon path.
     */
    Map<String, String> getIconPathMap();

    /**
     * Maps the principal type to the icon alt text.
     */
    Map<String, String> getIconAltMap();

    Boolean getBlockRightInheritance();

    void setBlockRightInheritance(Boolean blockRightInheritance);

    /**
     * @deprecated use {@link #getDisplayInheritedPermissions()}
     */
    @Deprecated
    Boolean displayInheritedPermissions();

    /**
     * Returns true if inherited permissions have to be displayed (depending on rights blocking)
     */
    boolean getDisplayInheritedPermissions();

    List<String> getCurrentDocumentUsers();

    List<String> getParentDocumentsUsers();

    String removePermissions();

    String saveSecurityUpdates();

    /**
     * Returns selected entry used in add/remove methods
     */
    String getSelectedEntry();

    /**
     * Sets selected entry used in add/remove methods
     */
    void setSelectedEntry(String selectedEntry);

    /**
     * Returns selected entries used in add/remove methods
     */
    List<String> getSelectedEntries();

    /**
     * Sets selected entries used in add/remove methods
     */
    void setSelectedEntries(List<String> selectedEntries);

}
