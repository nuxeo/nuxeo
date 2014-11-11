/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.query.sql.model.SQLQuery.Transformer;

/**
 * Interface for pluggable core security policy.
 *
 * @author Anahide Tchertchian
 * @author Florent Guillaume
 */
public interface SecurityPolicy {

    /**
     * Checks given permission for doc and principal.
     * <p>
     * Note that for the {@code Browse} permission, which is also implemented
     * in SQL using {@link #getQueryTransformer}, a security policy must never
     * bypass standard ACL access, it must only return DENY or UNKNOWN. Failing
     * to do this would make direct access and queries behave differently.
     *
     * @param doc the document to check
     * @param mergedAcp merged ACP resolved for this document
     * @param principal principal to check
     * @param permission permission to check
     * @param resolvedPermissions permissions or groups of permissions
     *            containing permission
     * @param additionalPrincipals
     * @return access: GRANT, DENY, or UNKNOWN. When UNKNOWN is returned,
     *         following policies or default core security are applied.
     */
    Access checkPermission(Document doc, ACP mergedAcp, Principal principal,
            String permission, String[] resolvedPermissions,
            String[] additionalPrincipals);

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
     * Checks if this policy can be expressed in a query for given repository.
     * <p>
     * If not, then any query made will have to be post-filtered.
     *
     * @param repositoryName the target repository name.
     * @return {@code true} if the policy can be expressed in a query
     */
    boolean isExpressibleInQuery(String repositoryName);

    /**
     * Checks if this policy can be expressed in a string-based query for given
     * repository.
     * <p>
     * If not, then any query made will have to be post-filtered, if possible,
     * otherwise denied.
     *
     * @param repositoryName the target repository name.
     * @return {@code true} if the policy can be expressed in a string-based
     *         query
     *
     * @since 5.7.2
     */
    boolean isExpressibleInQuery(String repositoryName, String queryLanguage);

    /**
     * Get the transformer to use to apply this policy to a query.
     * <p>
     * Called only when {@link #isExpressibleInQuery()} returned {@code true}
     *
     * @param repositoryName the target repository name.
     * @return the transformer
     */
    Transformer getQueryTransformer(String repositoryName);

    /**
     * Get the string-based transformer to use to apply this policy to a query.
     * <p>
     * Called only when {@link #isExpressibleInQuery(String, String)} returned
     * {@code true}
     *
     * @param repositoryName the target repository name.
     * @return the transformer
     *
     * @since 5.7.2
     */
    QueryTransformer getQueryTransformer(String repositoryName,
            String queryLanguage);

    /**
     * Interface for a class that can transform a string-based query into
     * another. Not used for NXQL.
     *
     * @since 5.7.2
     */
    interface QueryTransformer {

        /**
         * Query transformer that does nothing.
         */
        QueryTransformer IDENTITY = new IdentityQueryTransformer();

        /**
         * Transforms a query into another query that has the security policy
         * applied.
         *
         * @param principal the principal making the query
         * @param query the query
         * @return the query with security policy applied
         *
         * @since 5.7.2
         */
        String transform(Principal principal, String query);
    }

    /**
     * Query transformer that does nothing. Use
     * {@link QueryTransformer#IDENTITY} instead of instantiating this class.
     *
     * @since 5.7.2
     */
    class IdentityQueryTransformer implements QueryTransformer {
        @Override
        public String transform(Principal principal, String query) {
            return query;
        }
    }

}
