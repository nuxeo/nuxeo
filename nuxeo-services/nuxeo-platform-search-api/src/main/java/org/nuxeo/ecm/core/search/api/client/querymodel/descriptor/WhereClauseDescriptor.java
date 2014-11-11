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

package org.nuxeo.ecm.core.search.api.client.querymodel.descriptor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.Escaper;
import org.nuxeo.ecm.core.search.api.client.querymodel.LuceneMinimalEscaper;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @deprecated: use content views instead
 */
@Deprecated
@XObject(value = "whereClause")
public class WhereClauseDescriptor {

    private static final Log log = LogFactory.getLog(WhereClauseDescriptor.class);

    @XNode("@escaper")
    protected String escaperClassName = "org.nuxeo.ecm.core.search.api.client.querymodel.LuceneMinimalEscaper";

    @XNodeList(value = "predicate", componentType = PredicateDescriptor.class, type = PredicateDescriptor[].class)
    protected PredicateDescriptor[] predicates;

    protected String fixedPart;

    @XNode("fixedPart")
    public void setFixedPath(String fixedPart) {
        // remove new lines and following spaces
        this.fixedPart = fixedPart.replaceAll("\r?\n\\s*", " ");
    }

    // default escaper instance
    protected Escaper escaper = new LuceneMinimalEscaper();

    public PredicateDescriptor[] getPredicates() {
        return predicates;
    }

    public void setPredicates(PredicateDescriptor[] predicates) {
        this.predicates = predicates;
    }

    public String getQueryElement(DocumentModel model) throws ClientException {
        List<String> elements = new ArrayList<String>();
        if (predicates != null) {
            for (PredicateDescriptor predicate : predicates) {
                String predicateString = predicate.getQueryElement(model,
                        escaper);
                if (predicateString == null) {
                    continue;
                }

                predicateString = predicateString.trim();
                if (!predicateString.equals("")) {
                    elements.add(predicateString);
                }
            }
        }

        // add fixed part if applicable
        if (fixedPart != null && !fixedPart.equals("")) {
            if (elements.isEmpty()) {
                elements.add(fixedPart.trim());
            } else {
                elements.add('(' + fixedPart.trim() + ')');
            }
        }

        if (elements.isEmpty()) {
            return "";
        }

        // XXX: for now only a one level implement conjunctive WHERE clause
        String clauseValues = StringUtils.join(elements, " AND ").trim();

        // GR: WHERE (x = 1) is invalid NXQL
        while (elements.size() == 1 && clauseValues.startsWith("(")
                && clauseValues.endsWith(")")) {
            clauseValues = clauseValues.substring(1, clauseValues.length() - 1).trim();
        }
        if (clauseValues.length() == 0) {
            return "";
        }
        return " WHERE " + clauseValues;
    }

    /**
     * Initiates escaper object by using the provided {@link RuntimeContext}.
     *
     * @param context
     */
    public void initEscaper(RuntimeContext context) {
        try {
            escaper = (Escaper) context.loadClass(escaperClassName).newInstance();
        } catch (InstantiationException e) {
            log.warn("Could not instantiate esacper: " + e.getMessage());
        } catch (IllegalAccessException e) {
            log.warn("Could not instantiate escaper: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            log.warn("escaper class " + escaperClassName + "not found");
        }
    }

    public String getFixedPart() {
        return fixedPart;
    }

    public void setFixedPart(String fixedPart) {
        this.fixedPart = fixedPart;
    }

}
