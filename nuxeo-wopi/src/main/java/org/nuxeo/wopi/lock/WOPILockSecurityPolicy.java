/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.wopi.lock;

import java.security.Principal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.security.LockSecurityPolicy;

/**
 * Security policy to allow collaborative edition with WOPI.
 * <p>
 * See <a href="https://wopi.readthedocs.io/en/latest/scenarios/coauth.html">Co-authoring using Office Online</a>.
 * <p>
 * Unlike the standard {@link LockSecurityPolicy}, even if the document is locked by someone else, the WRITE permission
 * is not blocked if a WOPI lock exists for the document and the current user is a WOPI user, i.e. the request
 * originated from a WOPI client.
 * <p>
 * This handles the case of multiple users editing a document at the same time in Office Online, which is considered by
 * the Nuxeo WOPI host as a single WOPI client.
 *
 * @since 10.3
 */
public class WOPILockSecurityPolicy extends LockSecurityPolicy {

    private static final Logger log = LogManager.getLogger(WOPILockSecurityPolicy.class);

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp, Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = super.checkPermission(doc, mergedAcp, principal, permission, resolvedPermissions,
                additionalPrincipals);
        String repositoryName = doc.getSession().getRepositoryName();
        String docUUID = doc.getUUID();
        if (Access.DENY.equals(access) && LockHelper.isLocked(repositoryName, docUUID)
                && LockHelper.isWOPIUser(principal)) {
            // locked by another user but WOPI lock and WOPI user, don't block
            log.debug(
                    "Security: repository={} docId={} user={} Document is locked by another user but it has a WOPI lock and the current user belongs to a WOPI session, don't block WRITE permission",
                    repositoryName, docUUID, principal);
            return Access.UNKNOWN;
        }
        return access;
    }

}
