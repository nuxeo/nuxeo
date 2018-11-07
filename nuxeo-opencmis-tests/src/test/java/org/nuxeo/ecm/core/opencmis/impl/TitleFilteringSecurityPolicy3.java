/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery.Transformer;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;

/**
 * Test security policy that forbids titles starting with SECRET.
 *
 * @since 6.0
 */
public class TitleFilteringSecurityPolicy3 extends TitleFilteringSecurityPolicy {

    protected static final String PREFIX = "SECRET";

    @Override
    public boolean isExpressibleInQuery(String repositoryName) {
        return true;
    }

    @Override
    public Transformer getQueryTransformer(String repositoryName) {
        return TitleFilteringTransformer.INSTANCE;
    }

    /**
     * Transformer that adds {@code AND dc:title NOT LIKE 'SECRET%'} to the query.
     */
    public static class TitleFilteringTransformer implements Transformer {

        private static final long serialVersionUID = 1L;

        public static final Transformer INSTANCE = new TitleFilteringTransformer();

        public static final Predicate NO_SECRET_TITLE = new Predicate(new Reference("dc:title"), Operator.NOTLIKE,
                new StringLiteral("SECRET%"));

        @Override
        public SQLQuery transform(NuxeoPrincipal principal, SQLQuery query) {
            WhereClause where = query.where;
            Predicate predicate;
            if (where == null || where.predicate == null) {
                predicate = NO_SECRET_TITLE;
            } else {
                predicate = new Predicate(NO_SECRET_TITLE, Operator.AND, where.predicate);
            }
            return query.withWhereExpression(predicate);
        }
    }

}
