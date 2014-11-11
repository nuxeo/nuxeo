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

    public static final String TABBED = "tabbed";

    public static final String SEARCH_ONLY = "search_only";

    public static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-0123456789";

    public String getUserListingMode() throws ClientException;

    @Factory(value = "userList", scope = EVENT)
    public DocumentModelList getUsers() throws ClientException;

    public void resetUsers() throws ClientException;

    public DocumentModel getSelectedUser() throws ClientException;

    public String viewUser() throws ClientException;

    public String viewUser(String userName) throws ClientException;

    public String searchUsers() throws ClientException;

    public void validateUserName(FacesContext context, UIComponent component,
            Object value);

    public void validatePassword(FacesContext context, UIComponent component,
            Object value);

    public String editUser() throws ClientException;

    public String deleteUser() throws ClientException;

    public String updateUser() throws ClientException;

    public String changePassword() throws ClientException;

    public String createUser() throws ClientException,
            UserAlreadyExistsException;

    public DocumentModel getNewUser() throws ClientException;

    public String getSearchString();

    public void setSearchString(String searchString);

    public Collection<String> getCatalogLetters();

    public void setSelectedLetter(String selectedLetter);

    public String getSelectedLetter();

    public String viewUsers() throws ClientException;

    public boolean getAllowEditUser() throws ClientException;

    public boolean getAllowChangePassword() throws ClientException;

    public boolean getAllowCreateUser() throws ClientException;

    public boolean getAllowDeleteUser() throws ClientException;

    public String clearSearch() throws ClientException;

    public boolean isSearchOverflow();

    // XXX: never used, not tested
    public DocumentModel getSearchUserModel() throws ClientException;

    // XXX: never used, not tested
    public String searchUsersAdvanced() throws ClientException;

    // XXX: never used, not tested
    public String clearSearchAdvanced() throws ClientException;


}
