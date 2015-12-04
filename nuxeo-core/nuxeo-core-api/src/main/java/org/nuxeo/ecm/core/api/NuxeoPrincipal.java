/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Class to represent a principal in Nuxeo. This class holds the list of roles and groups for this principal.
 */
public interface NuxeoPrincipal extends Principal, Serializable {

    String PREFIX = "user:";

    /**
     * @since 8.1
     */
    String TRANSIENT_USER_PREFIX = "transient/";

    /**
     * @since 8.1
     */
    String TRANSIENT_USER_FORMAT = TRANSIENT_USER_PREFIX + "%s/%s";

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
     * Note: Some APIs that return principals from the database intentionally do not fill this field
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
     *
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

    void setModel(DocumentModel model);

    /**
     * Returns true if the principal is an administrator.
     * <p>
     * Security checks still apply on the repository for administrator user. If user is a system user, this method will
     * return true.
     *
     * @return true if the principal is an administrator.
     */
    boolean isAdministrator();

    /**
     * Returns the {@code tenantId} of this {@NuxeoPrincipal}, or {@code null} if there is no
     * {@code tenantId}.
     *
     * @since 5.6
     */
    String getTenantId();

    /**
     * Checks if the principal is anonymous (guest user).
     *
     * @return true if the principal is anonymous.
     */
    boolean isAnonymous();

    /**
     * Gets the base user from which this principal was created, or {@code null} if this principal was not created from
     * another user.
     *
     * @return the originating user, or {@code null}
     */
    String getOriginatingUser();

    /**
     * Sets the originating user.
     *
     * @param originatingUser the originating user
     */
    void setOriginatingUser(String originatingUser);

    /**
     * Gets the acting user for this principal.
     * <p>
     * This is the originating user (usually when this principal is a system user), or if there is none this principal's
     * user.
     *
     * @return the acting user
     * @since 6.0
     */
    String getActingUser();

    /**
     * Returns true if the principal is a transient principal.
     *
     * @since 8.1
     */
    boolean isTransient();

    /**
     * Returns true if the given @{code username} is a transient username.
     *
     * @since 8.1
     */
    static boolean isTransientUsername(String username) {
        return username != null && username.startsWith(TRANSIENT_USER_PREFIX);
    }

    /**
     * Computes a unique transient username from the given {@code baseUsername}.
     *
     * @since 8.1
     */
    static String computeTransientUsername(String baseUsername) {
        if (baseUsername != null && !baseUsername.startsWith(TRANSIENT_USER_PREFIX)) {
            String uuid = UUID.randomUUID().toString();
            uuid = uuid.replaceAll("-", "").substring(0, 16);
            return String.format(TRANSIENT_USER_FORMAT, baseUsername, uuid);
        }
        return baseUsername;
    }

}
