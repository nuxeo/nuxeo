package org.nuxeo.elasticsearch.test;/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Delbosc Benoit
 */


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
