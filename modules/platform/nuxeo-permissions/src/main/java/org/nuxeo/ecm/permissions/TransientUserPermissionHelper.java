/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @since 8.1
 */
public class TransientUserPermissionHelper {

    /**
     * @since 10.3
     */
    // status = 0 is PENDING, status = 1 or status = NULL is EFFECTIVE, excludes ARCHIVED ACLs
    public static final String OTHER_DOCUMENT_WITH_PENDING_OR_EFFECTIVE_ACL_QUERY = "SELECT ecm:uuid FROM Document, Relation"
            + " WHERE (ecm:acl/*1/status is NULL OR ecm:acl/*1/status = 0 OR ecm:acl/*1/status = 1)"
            + " AND ecm:acl/*1/principal = %s AND ecm:uuid <> %s";

    /**
     * @since 10.3
     */
    public static final String TRANSIENT_APP_NAME = "transient/appName";

    /**
     * @since 10.3
     */
    public static final String TRANSIENT_DEVICE_ID = "transient/deviceId";

    /**
     * @since 10.3
     */
    public static final String TRANSIENT_PERMISSION = "transient/permission";

    private TransientUserPermissionHelper() {
        // helper class
    }

    /**
     * @deprecated since 10.3. Use {@link #addToken(String)} instead.
     */
    @Deprecated
    public static String acquireToken(String username, DocumentModel doc, String permission) {
        addToken(username);
        // return value was never used anyway
        return null;
    }

    /**
     * Adds a token for the given {@code username}.
     * <p>
     * Does nothing if {@code username} is not a transient username or if a token already exists.
     *
     * @since 10.3
     */
    public static void addToken(String username) {
        if (NuxeoPrincipal.isTransientUsername(username)) {
            TokenAuthenticationService tokenAuthenticationService = Framework.getService(
                    TokenAuthenticationService.class);
            tokenAuthenticationService.acquireToken(username, TRANSIENT_APP_NAME, TRANSIENT_DEVICE_ID, null,
                    TRANSIENT_PERMISSION);
        }
    }

    /**
     * Returns the token linked to the given transient {@code username}, or {@code null} if no token can be found.
     *
     * @since 10.3
     */
    public static String getToken(String username) {
        return Framework.getService(TokenAuthenticationService.class)
                        .getToken(username, TRANSIENT_APP_NAME, TRANSIENT_DEVICE_ID);
    }

    public static void revokeToken(String username, DocumentModel doc) {
        if (NuxeoPrincipal.isTransientUsername(username)) {
            if (hasOtherPermission(username, doc)) {
                // do not remove the token as username has a permission on another document
                return;
            }

            // check if the transient user has other ACE on the document
            ACP acp = doc.getACP();
            for (ACL acl : acp.getACLs()) {
                if (ACL.INHERITED_ACL.equals(acl.getName())) {
                    continue;
                }

                for (ACE ace : acl) {
                    if (username.equals(ace.getUsername()) && !ace.isArchived()) {
                        // skip token removal
                        return;
                    }
                }
            }

            TokenAuthenticationService tokenAuthenticationService = Framework.getService(
                    TokenAuthenticationService.class);
            String token = tokenAuthenticationService.getToken(username, TRANSIENT_APP_NAME, TRANSIENT_DEVICE_ID);
            if (token != null) {
                tokenAuthenticationService.revokeToken(token);
            }

            // for compatibility, remove also token that may be stored based on the document
            token = tokenAuthenticationService.getToken(username, doc.getRepositoryName(), doc.getId());
            if (token != null) {
                tokenAuthenticationService.revokeToken(token);
            }
        }
    }

    /**
     * Returns {@code true} if the given {@code username} has a non-archived ACE on another document than {@code doc},
     * {@code false} otherwise.
     * <p>
     * Always returns {@code false} if the configuration property {@link NuxeoPrincipal#TRANSIENT_USERNAME_UNIQUE_PROP}
     * is {@code true}.
     *
     * @since 10.3
     */
    protected static boolean hasOtherPermission(String username, DocumentModel doc) {
        if (Framework.getService(ConfigurationService.class)
                     .isBooleanTrue(NuxeoPrincipal.TRANSIENT_USERNAME_UNIQUE_PROP)) {
            // as the transient username is unique, assume there is no other document with a permission
            // for username.
            return false;
        }

        String query = String.format(OTHER_DOCUMENT_WITH_PENDING_OR_EFFECTIVE_ACL_QUERY, NXQL.escapeString(username),
                NXQL.escapeString(doc.getId()));
        return CoreInstance.doPrivileged(doc.getRepositoryName(),
                session -> !session.queryProjection(query, 1, 0).isEmpty());
    }
}
