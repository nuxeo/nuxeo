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

import java.util.Collection;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.faces.model.SelectItem;

import org.jboss.seam.annotations.Destroy;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * Provides user manager related operations.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Local
@Remote
public interface UserManagerActions extends StatefulBaseLifeCycle {

    void initialize();

    String createUser() throws ClientException;

    String deleteUser() throws ClientException;

    String searchUsers() throws ClientException;

    String searchUsersAdvanced() throws ClientException;

    String clearSearchAdvanced() throws ClientException;

    void getUsers() throws ClientException;

    String editUser() throws ClientException;

    String viewUser() throws ClientException;

    String viewUser(String userName) throws ClientException;

    String updateUser() throws ClientException;

    String saveUser() throws ClientException;

    List<SelectItem> getAvailableGroups() throws ClientException;

    String getSearchString();

    void setSearchString(String searchString);

    String getSearchUsername();

    void setSearchUsername(String username);

    String getSearchFirstname();

    void setSearchFirstname(String firstName);

    String getSearchLastname();

    void setSearchLastname(String lastName);

    String getSearchEmail();

    void setSearchEmail(String email);

    String getSearchCompany();

    void setSearchCompany(String email);

    boolean getAllowDeleteUser() throws ClientException;

    boolean getAllowEditUser();

    boolean getAllowCreateUser() throws ClientException;

    boolean getAllowChangePassword() throws ClientException;

    String getRetypedPassword();

    void setRetypedPassword(String retypedPassword);

    String viewUsers() throws ClientException;

    String getSelectedLetter();

    void setSelectedLetter(String selectedLetter);

    Collection<String> getCatalogLetters();

    boolean getDoSearch();

    void setDoSearch(boolean doSearch);

    @Remove
    @Destroy
    @PermitAll
    void destroy();

    String clearSearch() throws ClientException;

    Type getChangeableUserType();
    Type getChangeableUserCreateType();

    void setChanged_password(String changed_password);

    void setChanged_password_verify(String changed_password_verify);

    String changePassword() throws ClientException;

    String getChanged_password();

    String getChanged_password_verify();

    boolean isSearchOverflow();

    void setSearchOverflow(boolean searchOverflow);

}
