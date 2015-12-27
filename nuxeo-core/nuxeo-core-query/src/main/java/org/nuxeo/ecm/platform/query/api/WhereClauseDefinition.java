/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
