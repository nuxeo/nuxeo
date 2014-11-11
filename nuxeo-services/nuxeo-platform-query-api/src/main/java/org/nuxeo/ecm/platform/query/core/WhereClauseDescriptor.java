/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Generic descriptor for query where clause, accepting predicates and a fixed
 * part. A custom escaper can also be set.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject(value = "whereClause")
public class WhereClauseDescriptor implements WhereClauseDefinition {

    /**
     * @deprecated since 5.9.6: doc type moved up to the page provider
     *             descriptor.
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
     * This parameter allows to override the default select statement used by
     * the fixed part ("select * from Document" for NXQL queries, for
     * instance).
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
     * @deprecated since 5.9.6: use
     *             {@link BasePageProviderDescriptor#getSearchDocumentType()}
     */
    @Deprecated
    public String getDocType() {
        return docType;
    }

    @XNode("fixedPart")
    public void setFixedPath(String fixedPart) {
        // remove new lines and following spaces
        this.fixedPart = fixedPart.replaceAll("\r?\n\\s*", " ");
    }

    public boolean getQuoteFixedPartParameters() {
        return quoteFixedPartParameters;
    }

    public boolean getEscapeFixedPartParameters() {
        return escapeFixedPartParameters;
    }

    public PredicateDefinition[] getPredicates() {
        return predicates;
    }

    public void setPredicates(PredicateDefinition[] predicates) {
        this.predicates = predicates;
    }

    public String getFixedPart() {
        return fixedPart;
    }

    public void setFixedPart(String fixedPart) {
        this.fixedPart = fixedPart;
    }

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
