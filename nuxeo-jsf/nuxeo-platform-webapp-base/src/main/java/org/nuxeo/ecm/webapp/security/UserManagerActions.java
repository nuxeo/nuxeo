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

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.Factory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;

/**
 * Provides user manager related operations.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Local
@Remote
public interface UserManagerActions extends Serializable {

    String TABBED = "tabbed";

    String SEARCH_ONLY = "search_only";

    String VALID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-0123456789.@";

    String getUserListingMode() throws ClientException;

    @Factory(value = "userList", scope = EVENT)
    DocumentModelList getUsers() throws ClientException;

    void resetUsers();

    DocumentModel getSelectedUser();

    String viewUser() throws ClientException;

    String viewUser(String userName) throws ClientException;

    String searchUsers() throws ClientException;

    void validateUserName(FacesContext context, UIComponent component,
            Object value);

    void validatePassword(FacesContext context, UIComponent component,
            Object value);

    String editUser() throws ClientException;

    String deleteUser() throws ClientException;

    String updateUser() throws ClientException;

    String changePassword() throws ClientException;

    String createUser() throws ClientException,
            UserAlreadyExistsException;

    DocumentModel getNewUser() throws ClientException;

    String getSearchString();

    void setSearchString(String searchString);

    Collection<String> getCatalogLetters();

    void setSelectedLetter(String selectedLetter);

    String getSelectedLetter();

    String viewUsers() throws ClientException;

    boolean getAllowEditUser() throws ClientException;

    boolean getAllowChangePassword() throws ClientException;

    boolean getAllowCreateUser() throws ClientException;

    boolean getAllowDeleteUser() throws ClientException;

    String clearSearch() throws ClientException;

    boolean isSearchOverflow();

    // XXX: never used, not tested
    DocumentModel getSearchUserModel() throws ClientException;

    // XXX: never used, not tested
    String searchUsersAdvanced() throws ClientException;

    // XXX: never used, not tested
    String clearSearchAdvanced() throws ClientException;

    boolean isNotReadOnly();

    List<String> getUserVirtualGroups(String userId) throws Exception;

}
