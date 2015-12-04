/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.1
 */
public class TransientUserPermissionHelper {

    private TransientUserPermissionHelper() {
        // helper class
    }

    public static String acquireToken(String username, DocumentModel doc, String permission) {
        if (NuxeoPrincipal.isTransientUsername(username)) {
            TokenAuthenticationService tokenAuthenticationService = Framework.getService(TokenAuthenticationService.class);
            return tokenAuthenticationService.acquireToken(username, doc.getRepositoryName(), doc.getId(), null,
                    permission);
        }
        return null;
    }

    public static void revokeToken(String username, DocumentModel doc) {
        if (NuxeoPrincipal.isTransientUsername(username)) {
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

            TokenAuthenticationService tokenAuthenticationService = Framework.getService(TokenAuthenticationService.class);
            String token = tokenAuthenticationService.getToken(username, doc.getRepositoryName(), doc.getId());
            if (token != null) {
                tokenAuthenticationService.revokeToken(token);
            }
        }
    }
}
