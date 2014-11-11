/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.security;

import java.security.Principal;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Security policy that blocks WRITE permission on a document if it is locked by
 * someone else.
 *
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public class LockSecurityPolicy extends AbstractSecurityPolicy {

    private static final Log log = LogFactory.getLog(LockSecurityPolicy.class);

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp,
            Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = Access.UNKNOWN;
        // policy only applies on WRITE
        if (resolvedPermissions == null
                || !Arrays.asList(resolvedPermissions).contains(
                        SecurityConstants.WRITE)) {
            return access;
        }
        // check the lock
        try {
            String username = principal.getName();
            Lock lock = doc.getLock();
            if (lock != null && !username.equals(lock.getOwner())) {
                // locked by another user => deny
                access = Access.DENY;
            }
        } catch (Exception e) {
            // ignore
            log.debug("Failed to get lock status on document ", e);
        }
        return access;
    }

    @Override
    public boolean isRestrictingPermission(String permission) {
        assert permission.equals("Browse"); // others not coded
        return false;
    }

    @Override
    public boolean isExpressibleInQuery() {
        return true;
    }

    @Override
    public SQLQuery.Transformer getQueryTransformer() {
        return SQLQuery.Transformer.IDENTITY;
    }

}
