/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.query.sql.model.IntegerLiteral;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.WhereClause;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.security.AbstractSearchPolicy;

/**
 * Search policy used for tests that adds search restrictions depending on the
 * user access level and confidentiality set on the document.
 *
 * @author Anahide Tchertchian
 */
public class AccessLevelSearchPolicy extends AbstractSearchPolicy {

    private static final Log log = LogFactory.getLog(AccessLevelSearchPolicy.class);

    public ComposedNXQuery applyPolicy(ComposedNXQuery nxqlQuery)
            throws SearchException {
        SQLQuery query = nxqlQuery.getQuery();
        NuxeoPrincipal principal = null;
        SearchPrincipal sPrincipal = nxqlQuery.getSearchPrincipal();
        if (sPrincipal != null) {
            principal = (NuxeoPrincipal) sPrincipal.getOriginalPrincipal();
        }
        if (principal == null) {
            return nxqlQuery;
        }
        Predicate add = new Predicate(new Reference("sp:securityLevel"),
                Operator.LTEQ, new IntegerLiteral(
                        (Long) principal.getModel().getProperty("user",
                                "accessLevel")));
        if (!query.getWhereClause().toString().contains(add.toString())) {
            WhereClause wc;
            if (query.getWhereClause() != null) {
                wc = new WhereClause(new Predicate(
                        query.getWhereClause().predicate, Operator.AND, add));
            } else {
                wc = new WhereClause(add);
            }
            query = new SQLQuery(query.getSelectClause(),
                    query.getFromClause(), wc, query.groupBy, query.having,
                    query.getOrderByClause());
        }
        log.debug(query);
        return new ComposedNXQueryImpl(query);
    }
}