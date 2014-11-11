/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.query.test;

import java.security.Principal;

import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery.Transformer;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;

/**
 * Dummy security policy denying all access to File objects with a query
 * transformer.
 *
 * @author Florent Guillaume
 */
public class NoFile2SecurityPolicy extends NoFileSecurityPolicy {

    @Override
    public boolean isExpressibleInQuery() {
        return true;
    }

    /**
     * Transformer that adds {@code AND ecm:primaryType <> 'File'} to the query.
     */
    public static class NoFileTransformer implements Transformer {
        private static final long serialVersionUID = 1L;

        public static final Predicate NO_FILE = new Predicate(new Reference(
                "ecm:primaryType"), Operator.NOTEQ, new StringLiteral("File"));

        public SQLQuery transform(Principal principal, SQLQuery query) {
            WhereClause where = query.where;
            Predicate predicate;
            if (where == null || where.predicate == null) {
                predicate = NO_FILE;
            } else {
                predicate = new Predicate(NO_FILE, Operator.AND,
                        where.predicate);
            }
            SQLQuery newQuery = new SQLQuery(query.select, query.from,
                    new WhereClause(predicate), query.groupBy, query.having,
                    query.orderBy, query.limit, query.offset);
            return newQuery;
        }
    }

    public static final Transformer NO_FILE_TRANSFORMER = new NoFileTransformer();

    @Override
    public Transformer getQueryTransformer() {
        return NO_FILE_TRANSFORMER;
    }

}
