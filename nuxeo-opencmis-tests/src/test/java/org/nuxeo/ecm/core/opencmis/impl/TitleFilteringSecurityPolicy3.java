/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * @since 5.9.6
 */
public class TitleFilteringSecurityPolicy3 extends TitleFilteringSecurityPolicy {

    protected static final String PREFIX = "SECRET";

    @Override
    public boolean isExpressibleInQuery() {
        return true;
    }

    @Override
    public Transformer getQueryTransformer() {
        return TitleFilteringTransformer.INSTANCE;
    }
    /**
     * Transformer that adds {@code AND dc:title NOT LIKE 'SECRET%'} to the
     * query.
     */
    public static class TitleFilteringTransformer implements Transformer {

        private static final long serialVersionUID = 1L;

        public static final Transformer INSTANCE = new TitleFilteringTransformer();

        public static final Predicate NO_SECRET_TITLE = new Predicate(
                new Reference("dc:title"), Operator.NOTLIKE, new StringLiteral(
                        "SECRET%"));

        @Override
        public SQLQuery transform(Principal principal, SQLQuery query) {
            WhereClause where = query.where;
            Predicate predicate;
            if (where == null || where.predicate == null) {
                predicate = NO_SECRET_TITLE;
            } else {
                predicate = new Predicate(NO_SECRET_TITLE, Operator.AND,
                        where.predicate);
            }
            SQLQuery newQuery = new SQLQuery(query.select, query.from,
                    new WhereClause(predicate), query.groupBy, query.having,
                    query.orderBy, query.limit, query.offset);
            return newQuery;
        }
    }

}
