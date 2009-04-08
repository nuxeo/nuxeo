/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.security;

import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;
import org.nuxeo.ecm.webapp.table.model.UserPermissionsTableModel;

/**
 * Provides security related operations on the current document.
 *
 * @author Razvan Caraghin
 */
public interface SecurityActions extends StatefulBaseLifeCycle {

    /**
     * Submits the security changes to the backend.
     *
     * @return the page that will be displayed next
     * @throws ClientException
     */
    String updateSecurityOnDocument() throws ClientException;

    /**
     * Adds a permission to the list of permissions for the current document.
     * After all client side changes are made, then the list of permissions need
     * to be sumitted on backend using <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     */
    String addPermission();

    /**
     * Adds a list of permission to the list of permissions for the current
     * document. After all client side changes are made, then the list of
     * permissions need to be sumitted on backend using
     * <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     */
    String addPermissions();

    String addPermission(String principalName, String permissionName,
            boolean grant);

    /**
     * Removes a permission from the list of permissions for the current
     * document. After all client side changes are made, then the list of
     * permissions need to be sumitted on backend using
     * <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     */
    String removePermission();

    /**
     * Adds a permission to the list of permissions for the current document and
     * automatically update the backend with
     * <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     * @throws ClientException
     */
    String addPermissionAndUpdate() throws ClientException;

    /**
     * Adds a list of permissions to the list of permissions for the current
     * document and automatically update the backend with
     * <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     * @throws ClientException
     */
    String addPermissionsAndUpdate() throws ClientException;

    /**
     * Removes a permission from the list of permissions for the current
     * document and automatically update the backend with
     * <code>updateSecurityOnDocument()></code>.
     *
     * @return the page that needs to be displayed next
     * @throws ClientException
     */
    String removePermissionAndUpdate() throws ClientException;

    String removePermissionsAndUpdate() throws ClientException;

    /**
     * Marks the current security data info as obsolete so that it gets lazily
     * recomputed from the backend the next time it is accessed.
     *
     * @throws ClientException
     */
    void resetSecurityData();

    /**
     * Rebuilds the security displayable data from the current selected
     * document.
     *
     * @throws ClientException
     */
    void rebuildSecurityData() throws ClientException;

    void destroy();

    /**
     *
     * @return a UserPermissionsTableModel used to build a checkboxable listing
     *         of managed permissions
     * @throws ClientException
     */
    UserPermissionsTableModel getDataTableModel() throws ClientException;

    /**
     * @return the SecurityData object that manages a stateful representation of
     *         the permissions mapping that apply to the current document
     *         (inherited or not)
     *
     * @throws ClientException
     */
    SecurityData getSecurityData() throws ClientException;

    /**
     * Returns true if the implementator if the principal has the permission to
     * add new security rules on currentItem.
     *
     * @return
     * @throws ClientException
     */
    boolean getCanAddSecurityRules() throws ClientException;

    /**
     * Returns true if the implementator can provide provide a list of
     * permissions delete now and the principal has WriteSecurity permission on
     * the currentItem.
     *
     * @return
     * @throws ClientException
     */
    boolean getCanRemoveSecurityRules() throws ClientException;

    /**
     * @return the list of permissions the users can set through the rights
     *         management tab
     * @throws ClientException
     */
    List<SelectItem> getSettablePermissions() throws ClientException;

    /**
     * Maps the principal type to the icon path.
     *
     * @return
     */
    Map<String, String> getIconPathMap();

    /**
     * Maps the principal type to the icon alt text.
     *
     * @return
     */
    Map<String, String> getIconAltMap();

    Boolean getBlockRightInheritance();

    void setBlockRightInheritance(Boolean blockRightInheritance)
            throws ClientException;

    /**
     * @deprecated use {@link #getDisplayInheritedPermissions()}
     */
    @Deprecated
    Boolean displayInheritedPermissions() throws ClientException;

    /**
     * Returns true if inherited permissions have to be displayed (depending on
     * rights blocking)
     *
     * @throws ClientException
     */
    boolean getDisplayInheritedPermissions() throws ClientException;

    List<String> getCurrentDocumentUsers() throws ClientException;

    List<String> getParentDocumentsUsers() throws ClientException;

    String removePermissions() throws ClientException;

    String saveSecurityUpdates() throws ClientException;

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
