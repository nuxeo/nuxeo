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

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * Service checking permissions for pluggable policies.
 *
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public interface SecurityPolicyService extends Serializable {

    /**
     * Checks given permission for doc and principal.
     * <p>
     * The security service checks this service for a security access. This
     * access is defined iterating over pluggable policies in a defined order.
     * If access is not specified, security service applies its default policy.
     *
     * @param doc the document to check
     * @param mergedAcp merged acp resolved for this document
     * @param principal principal to check
     * @param permission permission to check
     * @param resolvedPermissions permissions or groups of permissions
     *            containing permission
     * @param principalsToCheck principals (groups) to check for principal
     * @return access: true, false, or nothing. When nothing is returned,
     *         following policies or default core security are applied.
     */
    Access checkPermission(Document doc, ACP mergedAcp, Principal principal,
            String permission, String[] resolvedPermissions,
            String[] principalsToCheck);

    void registerDescriptor(SecurityPolicyDescriptor descriptor);

    void unregisterDescriptor(SecurityPolicyDescriptor descriptor);

    /**
     * Checks if any policy restricts the given permission.
     * <p>
     * If not, then no post-filtering on policies will be needed for query
     * results.
     *
     * @return {@code true} if a policy restricts the permission
     */
    boolean arePoliciesRestrictingPermission(String permission);

    /**
     * Checks if the policies can be expressed in a query for a given
     * repository.
     * <p>
     * If not, then any query made will have to be post-filtered.
     *
     * @param repositoryName the target repository name.
     * @return {@code true} if all policies can be expressed in a query
     */
    boolean arePoliciesExpressibleInQuery(String repositoryName);

    /**
     * Get the transformers to apply the policies to a query for given
     * repository.
     *
     * @param repositoryName the target repository name.
     * @return the transformers.
     */
    Collection<SQLQuery.Transformer> getPoliciesQueryTransformers(
            String repositoryName);

}
