/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.document;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelListCollector;
import org.nuxeo.ecm.automation.core.events.DocumentAttributeFilterFactory;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = FilterDocuments.ID, category = Constants.CAT_DOCUMENT, label = "Filter List", description = "Filter the input list of documents given a condition. The condition can be expressed using 4 parameters: types, facets, lifecycle and condition. If more than one parameter is specified an AND will be used to group conditions. <br>The 'types' parameter can take a comma separated list of document type: File,Note.<br>The 'facet' parameter can take a single facet name.<br> The 'life cycle' parameter takes a name of a life cycle state the document should have.<br>The 'condition' parameter can take any EL expression.<p>Returns the list of documents that match the filter condition.")
public class FilterDocuments {

    private static final Log log = LogFactory.getLog(FilterDocuments.class);

    public static final String ID = "Document.Filter";

    @Context
    protected OperationContext ctx;

    @Param(name = "types", required = false)
    protected String types; // comma separated list.

    @Param(name = "facet", required = false)
    protected String facet;

    @Param(name = "lifecycle", required = false)
    protected String lifeCycle;

    @Param(name = "pathStartsWith", required = false)
    protected String pathStartsWith;

    @Param(name = "condition", required = false)
    protected String condition;

    @Param(name = "class", required = false, widget = Constants.W_OPTION, values = {
            DocumentAttributeFilterFactory.ANY_DOC, DocumentAttributeFilterFactory.REGULAR_DOC,
            DocumentAttributeFilterFactory.LINK_DOC, DocumentAttributeFilterFactory.PUBLISHED_DOC,
            DocumentAttributeFilterFactory.PROXY_DOC, DocumentAttributeFilterFactory.VERSION_DOC,
            DocumentAttributeFilterFactory.IMMUTABLE_DOC, DocumentAttributeFilterFactory.MUTABLE_DOC })
    protected String attr;

    @OperationMethod(collector = DocumentModelListCollector.class)
    public DocumentModelList run(DocumentModel doc) {
        // Method rewritten to use Collector in order to execute condition on each document
        Condition cond = new Condition();
        DocumentModelList ret = new DocumentModelListImpl();
        if (cond.accept(doc)) {
            ret.add(doc);
        }
        return ret;
    }

    protected class Condition implements Filter {

        Set<String> types;

        String facet;

        String lc;

        String path;

        Expression expr;

        Filter attr;

        Condition() {
            String v = FilterDocuments.this.types;
            if (v != null) {
                v = v.trim();
                if (v.length() > 0) {
                    types = new HashSet<>();
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
            v = lifeCycle;
            if (v != null) {
                v = v.trim();
                if (v.length() > 0) {
                    lc = v;
                }
            }
            v = pathStartsWith;
            if (v != null) {
                v = v.trim();
                if (v.length() > 0) {
                    path = v;
                }
            }
            v = condition;
            if (v != null) {
                v = v.trim();
                if (v.length() > 0) {
                    expr = Scripting.newExpression(v);
                }
            }
            v = FilterDocuments.this.attr;
            if (v != null) {
                attr = DocumentAttributeFilterFactory.getFilter(v);
            }
        }

        @Override
        public boolean accept(DocumentModel doc) {
            if (types != null) {
                if (!types.contains(doc.getType())) {
                    return false;
                }
            }
            if (facet != null) {
                if (!doc.getFacets().contains(facet)) {
                    return false;
                }
            }
            if (lc != null) {
                if (!lc.equals(doc.getCurrentLifeCycleState())) {
                    return false;
                }
            }
            if (path != null) {
                if (!doc.getPathAsString().startsWith(path)) {
                    return false;
                }
            }
            if (attr != null) {
                if (!attr.accept(doc)) {
                    return false;
                }
            }
            if (expr != null) {
                try {
                    if (!(Boolean) expr.eval(ctx)) {
                        return false;
                    }
                } catch (RuntimeException e) {
                    log.error(e, e);
                }
            }
            return true;
        }
    }

}
