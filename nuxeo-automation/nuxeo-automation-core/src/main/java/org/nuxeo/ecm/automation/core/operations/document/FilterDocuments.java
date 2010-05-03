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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.events.AttrFilterFactory;
import org.nuxeo.ecm.automation.core.events.Filter;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * Save the input document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Operation(id=FilterDocuments.ID, category=Constants.CAT_DOCUMENT, label="Filter List",
        description="Filter the input list of documents given a condition. The condition can be expressed using 4 parameters: types, facets, lifecycle and condition. If more than one parameter is specified an AND will be used to group conditions. <br>The 'types' paramter can take a comma separated list of document type: File,Note.<br>The 'facet' parameter can take a single facet name.<br> The 'life cycle' parameter takes a name of a life cycle state the document should have.<br>The 'condition' parameter can take any EL expression.<p>Returns the list of documents that match the filter condition.")
public class FilterDocuments {

    public static final String ID = "Document.Filter";


    protected @Context OperationContext ctx;

    protected @Param(name="types", required=false) String types; // comma separated list.
    protected @Param(name="facet", required=false) String facet;
    protected @Param(name="lifecycle", required=false) String lifeCycle;
    protected @Param(name="condition", required=false) String condition;
    @Param(name="class", required=false, widget=Constants.W_OPTION, values={
            AttrFilterFactory.ANY_DOC,
            AttrFilterFactory.REGULAR_DOC,
            AttrFilterFactory.LINK_DOC,
            AttrFilterFactory.PUBLISHED_DOC,
            AttrFilterFactory.PROXY_DOC,
            AttrFilterFactory.VERSION_DOC,
            AttrFilterFactory.IMMUTABLE_DOC,
            AttrFilterFactory.MUTABLE_DOC
            })
    protected String attr;

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) throws Exception {
        Condition cond = new Condition();
        DocumentModelList result = new DocumentModelListImpl();
        for (DocumentModel doc : docs) {
            if (cond.accept(doc)) {
                result.add(doc);
            }
        }
        return result;
    }


    protected class Condition implements Filter {
        Set<String> types;
        String facet;
        String lc;
        Expression expr;
        Filter attr;

        Condition() {
            String v = FilterDocuments.this.types;
            if (v != null) {
                v = v.trim();
                if (v.length() > 0) {
                    types = new HashSet<String>();
                    for (String t : StringUtils.split(v, ',', true)) {
                        types.add(t);
                    }
                }
            }
            v = FilterDocuments.this.facet;
            if (v != null) {
                v = v.trim();
                if (v.length() > 0) {
                    facet = v;
                }
            }
            v = FilterDocuments.this.lifeCycle;
            if (v != null) {
                v = v.trim();
                if (v.length() > 0) {
                    lc = v;
                }
            }
            v = FilterDocuments.this.condition;
            if (v != null) {
                v = v.trim();
                if (v.length() > 0) {
                    expr = Scripting.newExpression(v);
                }
            }
            v = FilterDocuments.this.attr;
            if (v != null) {
                attr = AttrFilterFactory.getFilter(v);
            }
        }

        public boolean accept(DocumentModel doc) throws Exception {
            if (types != null) {
                if (!types.contains(doc.getType())) {
                    return false;
                }
            }
            if (facet != null) {
                if (!doc.getDeclaredFacets().contains(facet)) {
                    return false;
                }
            }
            if (lc != null) {
                if (!lc.equals(doc.getCurrentLifeCycleState())) {
                    return false;
                }
            }
            if (attr != null) {
                if (!attr.accept(doc)) {
                    return false;
                }
            }
            if (expr != null) {
                if (!(Boolean)expr.eval(ctx)) {
                    return false;
                }
            }
            return true;
        }
    }


}
