/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.query.core;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.search.api.client.querymodel.Escaper;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;

/**
 * Generic descriptor for query where clause, accepting predicates and a fixed part. A custom escaper can also be set.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject(value = "whereClause")
public class WhereClauseDescriptor implements WhereClauseDefinition {

    /**
     * @deprecated since 6.0: doc type moved up to the page provider descriptor.
     */
    @Deprecated
    @XNode("@docType")
    protected String docType;

    @XNode("@escaper")
    protected Class<? extends Escaper> escaperClass;

    @XNodeList(value = "predicate", componentType = PredicateDescriptor.class, type = PredicateDefinition[].class)
    protected PredicateDefinition[] predicates;

    protected String fixedPart;

    /**
     * This parameter allows to override the default select statement used by the fixed part ("select * from Document"
     * for NXQL queries, for instance).
     *
     * @since 5.9.2
     */
    @XNode("fixedPart@statement")
    protected String selectStatement;

    @XNode("fixedPart@quoteParameters")
    protected boolean quoteFixedPartParameters = true;

    @XNode("fixedPart@escape")
    protected boolean escapeFixedPartParameters = true;

    /**
     * @deprecated since 6.0: use {@link BasePageProviderDescriptor#getSearchDocumentType()}
     */
    @Override
    @Deprecated
    public String getDocType() {
        return docType;
    }

    @Override
    @XNode("fixedPart")
    public void setFixedPath(String fixedPart) {
        // remove new lines and following spaces
        this.fixedPart = fixedPart.replaceAll("\r?\n\\s*", " ");
    }

    @Override
    public boolean getQuoteFixedPartParameters() {
        return quoteFixedPartParameters;
    }

    @Override
    public boolean getEscapeFixedPartParameters() {
        return escapeFixedPartParameters;
    }

    @Override
    public PredicateDefinition[] getPredicates() {
        return predicates;
    }

    @Override
    public void setPredicates(PredicateDefinition[] predicates) {
        this.predicates = predicates;
    }

    @Override
    public String getFixedPart() {
        return fixedPart;
    }

    @Override
    public void setFixedPart(String fixedPart) {
        this.fixedPart = fixedPart;
    }

    @Override
    public Class<? extends Escaper> getEscaperClass() {
        return escaperClass;
    }

    @Override
    public String getSelectStatement() {
        return selectStatement;
    }

    /**
     * @since 5.6
     */
    @Override
    public WhereClauseDescriptor clone() {
        WhereClauseDescriptor clone = new WhereClauseDescriptor();
        clone.docType = getDocType();
        clone.escaperClass = getEscaperClass();
        if (predicates != null) {
            clone.predicates = new PredicateDefinition[predicates.length];
            for (int i = 0; i < predicates.length; i++) {
                clone.predicates[i] = predicates[i].clone();
            }
        }
        clone.fixedPart = fixedPart;
        clone.quoteFixedPartParameters = quoteFixedPartParameters;
        clone.escapeFixedPartParameters = escapeFixedPartParameters;
        clone.selectStatement = selectStatement;
        return clone;
    }
}
