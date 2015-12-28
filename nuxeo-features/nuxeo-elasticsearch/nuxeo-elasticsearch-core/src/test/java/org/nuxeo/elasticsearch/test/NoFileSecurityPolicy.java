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
 *     Delbosc Benoit
 */
package org.nuxeo.elasticsearch.test;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.StringLiteral;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.security.AbstractSecurityPolicy;
import org.nuxeo.ecm.core.security.SecurityPolicy;

import java.security.Principal;

/**
 * Dummy security policy denying all access to File objects for non Admin.
 *
 */
public class NoFileSecurityPolicy extends AbstractSecurityPolicy implements SecurityPolicy {
    public static final SQLQuery.Transformer NO_FILE_TRANSFORMER = new NoFileTransformer();

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp, Principal principal, String permission,
                                  String[] resolvedPermissions, String[] additionalPrincipals) {
        if (doc.getType().getName().equals("File")) {
            return Access.DENY;
        }
        return Access.UNKNOWN;
    }

    @Override
    public boolean isRestrictingPermission(String permission) {
        return true;
    }

    public SQLQuery.Transformer getQueryTransformer(String repositoryName) {
        return NO_FILE_TRANSFORMER;
    }

    public static class NoFileTransformer implements SQLQuery.Transformer {
        private static final long serialVersionUID = 1L;
        public static final Predicate NO_FILE;

        public NoFileTransformer() {
        }

        public SQLQuery transform(Principal principal, SQLQuery query) {
            if (principal instanceof NuxeoPrincipal && ((NuxeoPrincipal) principal).isAdministrator()) {
                return query;
            }
            WhereClause where = query.where;
            Predicate predicate;
            if (where != null && where.predicate != null) {
                predicate = new Predicate(NO_FILE, Operator.AND, where.predicate);
            } else {
                predicate = NO_FILE;
            }

            SQLQuery newQuery = new SQLQuery(query.select, query.from, new WhereClause(predicate), query.groupBy, query.having, query.orderBy, query.limit, query.offset);
            return newQuery;
        }

        static {
            NO_FILE = new Predicate(new Reference("ecm:primaryType"), Operator.NOTEQ, new StringLiteral("File"));
        }
    }

    @Override
    public boolean isExpressibleInQuery(String repositoryName) {
        return true;
    }
}
