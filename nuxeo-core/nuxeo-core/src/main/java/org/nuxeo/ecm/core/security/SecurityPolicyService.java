/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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
     * The security service checks this service for a security access. This access is defined iterating over pluggable
     * policies in a defined order. If access is not specified, security service applies its default policy.
     *
     * @param doc the document to check
     * @param mergedAcp merged acp resolved for this document
     * @param principal principal to check
     * @param permission permission to check
     * @param resolvedPermissions permissions or groups of permissions containing permission
     * @param principalsToCheck principals (groups) to check for principal
     * @return access: true, false, or nothing. When nothing is returned, following policies or default core security
     *         are applied.
     */
    Access checkPermission(Document doc, ACP mergedAcp, NuxeoPrincipal principal, String permission,
            String[] resolvedPermissions, String[] principalsToCheck);

    void registerDescriptor(SecurityPolicyDescriptor descriptor);

    void unregisterDescriptor(SecurityPolicyDescriptor descriptor);

    /**
     * Checks if any policy restricts the given permission.
     * <p>
     * If not, then no post-filtering on policies will be needed for query results.
     *
     * @return {@code true} if a policy restricts the permission
     */
    boolean arePoliciesRestrictingPermission(String permission);

    /**
     * Checks if the policies can be expressed in a query for a given repository.
     * <p>
     * If not, then any query made will have to be post-filtered.
     *
     * @param repositoryName the target repository name.
     * @return {@code true} if all policies can be expressed in a query
     */
    boolean arePoliciesExpressibleInQuery(String repositoryName);

    /**
     * Get the transformers to apply the policies to a query for given repository.
     *
     * @param repositoryName the target repository name.
     * @return the transformers.
     */
    Collection<SQLQuery.Transformer> getPoliciesQueryTransformers(String repositoryName);

    /**
     * Gets the list of registered security policies.
     *
     * @return the policies
     * @since 5.7.2
     */
    List<SecurityPolicy> getPolicies();

}
