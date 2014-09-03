/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.security.Principal;

import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * Test security policy that forbids titles starting with SECRET. This one can
 * be expressed in CMISQL.
 *
 * @since 5.7.2
 */
public class TitleFilteringSecurityPolicy2 extends TitleFilteringSecurityPolicy {

    private static final String CMISQL = "CMISQL";

    @Override
    public boolean isExpressibleInQuery(String repositoryName,
            String queryLanguage) {
        return NXQL.NXQL.equals(queryLanguage) || CMISQL.equals(queryLanguage);
    }

    @Override
    public QueryTransformer getQueryTransformer(String repositoryName,
            String queryLanguage) {
        if (!CMISQL.equals(queryLanguage)) {
            throw new UnsupportedOperationException(queryLanguage);
        }
        return TitleFilteringTransformer.INSTANCE;
    }

    /**
     * Transformer that adds {@code AND dc:title NOT LIKE 'SECRET%'} to the
     * query.
     */
    public static class TitleFilteringTransformer implements QueryTransformer {

        public static final QueryTransformer INSTANCE = new TitleFilteringTransformer();

        @Override
        public String transform(Principal principal, String statement) {
            /*
             * If you use this method as an example for a real QueryTransform
             * implementation, note that you should likely implement a more
             * complex logic to add things to the query. In particular, if the
             * query uses column aliases (qualifiers) or JOINs, then this has to
             * be taken into account to use them correctly to refer to the
             * columns needed to do the security checks (dc:title in this
             * example).
             */
            String securityClause = "dc:title NOT LIKE '" + PREFIX + "%'";
            String sep = statement.toUpperCase().contains(" WHERE ") ? " AND "
                    : " WHERE ";
            return statement + sep + securityClause;
        }
    }

}
