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

    public String getDocType();

    public void setFixedPath(String fixedPart);

    public boolean getQuoteFixedPartParameters();

    public boolean getEscapeFixedPartParameters();

    public PredicateDefinition[] getPredicates();

    public void setPredicates(PredicateDefinition[] predicates);

    public String getFixedPart();

    public void setFixedPart(String fixedPart);

    public Class<? extends Escaper> getEscaperClass();

}