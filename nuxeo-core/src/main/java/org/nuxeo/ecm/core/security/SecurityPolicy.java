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

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Interface for pluggable core security policy.
 *
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public interface SecurityPolicy {

    /**
     * Checks given permission for doc and principal.
     *
     * @param doc the document to check
     * @param mergedAcp merged ACP resolved for this document
     * @param principal principal to check
     * @param permission permission to check
     * @param resolvedPermissions permissions or groups of permissions
     *            containing permission
     * @param additionalPrincipals
     * @return access: true, false, or nothing. When nothing is returned,
     *         following policies or default core security are applied.
     * @throws SecurityException
     */
    Access checkPermission(Document doc, ACP mergedAcp, Principal principal,
            String permission, String[] resolvedPermissions,
            String[] additionalPrincipals) throws SecurityException;

    /**
     * Checks if this policy is restricting the given permission.
     * <p>
     * Queries check the BROWSE permission.
     *
     * @param permission the permission to check for
     * @return {@code true} if the policy restricts the permission
     */
    boolean isRestrictingPermission(String permission);

    /**
     * Checks if this policy can be expressed in a query.
     * <p>
     * If not, then any query made will have to be post-filtered.
     *
     * @return {@code true} if the policy can be expressed in a query
     */
    boolean isExpressibleInQuery();

    /**
     * Get the transformer to use to apply this policy to a query.
     * <p>
     * Called only when {@link #isExpressibleInQuery()} returned {@code true}
     *
     * @return the transformer
     */
    SQLQuery.Transformer getQueryTransformer();

}
