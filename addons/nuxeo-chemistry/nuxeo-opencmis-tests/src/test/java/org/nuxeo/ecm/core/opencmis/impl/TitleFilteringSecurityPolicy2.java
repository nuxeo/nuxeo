/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.query.sql.NXQL;

/**
 * Test security policy that forbids titles starting with SECRET. This one can be expressed in CMISQL.
 *
 * @since 5.7.2
 */
public class TitleFilteringSecurityPolicy2 extends TitleFilteringSecurityPolicy {

    private static final String CMISQL = "CMISQL";

    @Override
    public boolean isExpressibleInQuery(String repositoryName, String queryLanguage) {
        return NXQL.NXQL.equals(queryLanguage) || CMISQL.equals(queryLanguage);
    }

    @Override
    public QueryTransformer getQueryTransformer(String repositoryName, String queryLanguage) {
        if (!CMISQL.equals(queryLanguage)) {
            throw new UnsupportedOperationException(queryLanguage);
        }
        return TitleFilteringTransformer.INSTANCE;
    }

    /**
     * Transformer that adds {@code AND dc:title NOT LIKE 'SECRET%'} to the query.
     */
    public static class TitleFilteringTransformer implements QueryTransformer {

        public static final QueryTransformer INSTANCE = new TitleFilteringTransformer();

        @Override
        public String transform(NuxeoPrincipal principal, String statement) {
            /*
             * If you use this method as an example for a real QueryTransform implementation, note that you should
             * likely implement a more complex logic to add things to the query. In particular, if the query uses column
             * aliases (qualifiers) or JOINs, then this has to be taken into account to use them correctly to refer to
             * the columns needed to do the security checks (dc:title in this example).
             */
            String securityClause = "dc:title NOT LIKE '" + PREFIX + "%'";
            String sep = statement.toUpperCase().contains(" WHERE ") ? " AND " : " WHERE ";
            return statement + sep + securityClause;
        }
    }

}
