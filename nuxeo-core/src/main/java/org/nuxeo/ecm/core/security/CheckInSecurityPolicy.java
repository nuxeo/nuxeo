/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.security;

import java.security.Principal;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Security policy that denies write access on a live document when it is in the
 * checked-in state.
 * <p>
 * The document must be checked out before modification is allowed.
 *
 * @since 5.4
 */
public class CheckInSecurityPolicy extends AbstractSecurityPolicy {

    private static final Log log = LogFactory.getLog(CheckInSecurityPolicy.class);

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp,
            Principal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        Access access = Access.UNKNOWN;
        if (Arrays.asList(resolvedPermissions).contains(
                SecurityConstants.WRITE_PROPERTIES)
                && !doc.isVersion() && !doc.isProxy()) {
            try {
                if (!doc.isCheckedOut()) {
                    access = Access.DENY;
                }
            } catch (DocumentException e) {
                log.debug("Failed to get checked-out status on document", e);
            }
        }
        return access;
    }

    @Override
    public boolean isRestrictingPermission(String permission) {
        return permission.equals(SecurityConstants.WRITE);
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
