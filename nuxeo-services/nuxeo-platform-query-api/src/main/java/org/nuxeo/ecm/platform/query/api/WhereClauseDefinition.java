/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.query.api;

import org.nuxeo.ecm.core.search.api.client.querymodel.Escaper;

public interface WhereClauseDefinition {

    String getDocType();

    void setFixedPath(String fixedPart);

    boolean getQuoteFixedPartParameters();

    boolean getEscapeFixedPartParameters();

    PredicateDefinition[] getPredicates();

    void setPredicates(PredicateDefinition[] predicates);

    String getFixedPart();

    void setFixedPart(String fixedPart);

    Class<? extends Escaper> getEscaperClass();

    /**
     * Return the custom select statement used by the fixed part ("select * from Document" for NXQL queries, for
     * instance).
     *
     * @since 5.9.2
     */
    String getSelectStatement();

}
