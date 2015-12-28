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
