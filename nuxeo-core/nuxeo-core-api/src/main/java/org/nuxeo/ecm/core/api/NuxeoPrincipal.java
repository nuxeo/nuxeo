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

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;

/**
 * Class to represent a principal in Nuxeo. This class holds the list of roles
 * and groups for this principal.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface NuxeoPrincipal extends Principal, Serializable {

    String PREFIX = "user:";

    /**
     * Gets the first name of this principal.
     *
     * @return the first name of this principal
     */
    String getFirstName();

    /**
     * Gets the last name of this principal.
     *
     * @return the last name of this principal
     */
    String getLastName();

    /**
     * Gets the password of this principal.
     * <p>
     * Note: Some APIs that return principals from the database intentionally do
     * not fill this field
     *
     * @return the password of this principal
     */
    String getPassword();

    /**
     * Gets the company name of this principal.
     *
     * @return the company name
     */
    String getCompany();

    /**
     * Get the user email if any. Return null if not email was specified
     * @return the user email or null if none
     */
    String getEmail();

    /**
     * Gets the groups this principal is directly member of.
     *
     * @return the list of the groups
     */
    List<String> getGroups();

    /**
     * Gets the groups this principal directly or undirectly is member of.
     *
     * @return the list of the groups
     */
    List<String> getAllGroups();

    /**
     * Recursively test if the user is member of this group.
     *
     * @param group The name of the group
     */
    boolean isMemberOf(String group);

    /**
     * Gets the roles for this principal.
     *
     * @return the list of the roles
     */
    List<String> getRoles();

    void setName(String name);

    void setFirstName(String firstName);

    void setLastName(String lastName);

    void setGroups(List<String> groups);

    void setRoles(List<String> roles);

    void setCompany(String company);

    void setPassword(String password);

    void setEmail(String email);

    /**
     * Returns a generated id that is unique for each principal instance.
     *
     * @return a unique string
     */
    String getPrincipalId();

    /**
     * Sets the principalId.
     *
     * @param principalId a new principalId for this instance
     */
    void setPrincipalId(String principalId);

    DocumentModel getModel();

    void setModel(DocumentModel model) throws ClientException;

    /**
     * Returns true if the principal is an administrator.
     * <p>
     * Security checks still apply on the repository for administrator user. If
     * user is a system user, this method will return true.
     *
     * @return true if the principal is an administrator.
     */
    boolean isAdministrator();

    /**
     * Checks if the principal is anonymous (guest user).
     *
     * @return true if the principal is anonymous.
     */
    boolean isAnonymous();

    String getOriginatingUser();

    void setOriginatingUser(String originatingUser);

}
