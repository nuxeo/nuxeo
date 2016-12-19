/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.security;

import java.security.Principal;

import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery.Transformer;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;

/**
 * Dummy security policy denying all access to File objects with a query transformer.
 *
 * @author Florent Guillaume
 */
public class NoFile2SecurityPolicy extends NoFileSecurityPolicy {

    @Override
    public boolean isExpressibleInQuery(String repositoryName) {
        return true;
    }

    /**
     * Transformer that adds {@code AND ecm:primaryType <> 'File'} to the query.
     */
    public static class NoFileTransformer implements Transformer {
        private static final long serialVersionUID = 1L;

        public static final Predicate NO_FILE = new Predicate(new Reference("ecm:primaryType"), Operator.NOTEQ,
                new StringLiteral("File"));

        @Override
        public SQLQuery transform(Principal principal, SQLQuery query) {
            WhereClause where = query.where;
            Predicate predicate;
            if (where == null || where.predicate == null) {
                predicate = NO_FILE;
            } else {
                predicate = new Predicate(NO_FILE, Operator.AND, where.predicate);
            }
            return new SQLQuery(query.select, query.from, new WhereClause(predicate), query.groupBy, query.having,
                    query.orderBy, query.limit, query.offset);
        }
    }

    public static final Transformer NO_FILE_TRANSFORMER = new NoFileTransformer();

    @Override
    public Transformer getQueryTransformer(String repositoryName) {
        return NO_FILE_TRANSFORMER;
    }

}
