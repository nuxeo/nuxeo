/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment.security;

import static org.nuxeo.ecm.core.query.sql.model.Predicates.and;
import static org.nuxeo.ecm.core.query.sql.model.Predicates.noteq;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.security.AbstractSecurityPolicy;
import org.nuxeo.ecm.core.security.SecurityPolicy;

/**
 * @since 11.5
 */
public class AccessFromCommentTextSecurityPolicy extends AbstractSecurityPolicy implements SecurityPolicy {

    public static final String DENY = "Deny";

    @Override
    public Access checkPermission(Document doc, ACP mergedAcp, NuxeoPrincipal principal, String permission,
            String[] resolvedPermissions, String[] additionalPrincipals) {
        String docType = doc.getType().getName();
        if (ANNOTATION_DOC_TYPE.equals(docType) || COMMENT_DOC_TYPE.equals(docType)) {
            String text = doc.getValue(COMMENT_TEXT).toString();
            return DENY.equals(text) ? Access.DENY : Access.UNKNOWN;
        }
        return Access.UNKNOWN;
    }

    @Override
    public boolean isExpressibleInQuery(String repositoryName) {
        return true;
    }

    @Override
    public SQLQuery.Transformer getQueryTransformer(String repositoryName) {
        return NO_FILE_TRANSFORMER;
    }

    public static final SQLQuery.Transformer NO_FILE_TRANSFORMER = new NoFileTransformer();

    /**
     * Sample Transformer that adds {@code AND comment:text <> 'Deny'} to the query.
     */
    public static class NoFileTransformer implements SQLQuery.Transformer {

        public static final Predicate NO_DENY = noteq(COMMENT_TEXT, DENY);

        @Override
        public SQLQuery transform(NuxeoPrincipal principal, SQLQuery query) {
            if (!principal.isAdministrator()) {
                WhereClause where = query.where;
                Predicate predicate;
                if (where == null || where.predicate == null) {
                    predicate = NO_DENY;
                } else {
                    // adds an AND comment:text <> 'Deny' to the WHERE clause
                    predicate = and(NO_DENY, where.predicate);
                }
                // return query with updated WHERE clause
                return new SQLQuery(query.select, query.from, new WhereClause(predicate), query.groupBy, query.having,
                        query.orderBy, query.limit, query.offset);
            }
            return query;
        }
    }
}
