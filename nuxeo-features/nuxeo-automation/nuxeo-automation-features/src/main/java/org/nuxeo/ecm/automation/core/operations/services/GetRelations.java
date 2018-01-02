/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.automation.core.operations.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = GetRelations.ID, category = Constants.CAT_SERVICES, label = "Get Linked Documents", description = "Get the relations for the input document. The 'outgoing' parameter ca be used to specify whether outgoing or incoming relations should be returned. Retuns a document list.", aliases = {
        "Relations.GetRelations" })
public class GetRelations {

    public static final String ID = "Document.GetLinkedDocuments";

    @Context
    protected CoreSession session;

    @Context
    protected RelationManager relations;

    @Param(name = "predicate")
    // TODO use a combo box?
    protected String predicate;

    @Param(name = "outgoing", required = false)
    protected boolean outgoing = true;

    @Param(name = "graphName", required = false)
    protected String graphName;

    @OperationMethod
    public DocumentModelList run(DocumentModel doc) {
        QNameResource res = getDocumentResource(doc);
        Resource predicate = getPredicate();
        return getDocuments(res, predicate);
    }

    protected QNameResource getDocumentResource(DocumentModel document) {
        return (QNameResource) relations.getResource(RelationConstants.DOCUMENT_NAMESPACE, document, null);
    }

    protected Resource getPredicate() {
        return predicate != null && predicate.length() > 0 ? new ResourceImpl(predicate) : null;
    }

    protected DocumentModelList getDocuments(QNameResource res, Resource predicate) {
        if (outgoing) {
            List<Statement> statements = getOutgoingStatements(res, predicate);
            DocumentModelList docs = new DocumentModelListImpl(statements.size());
            for (Statement st : statements) {
                DocumentModel dm = getDocumentModel(st.getObject());
                if (dm != null) {
                    docs.add(dm);
                }
            }
            return docs;
        } else {
            List<Statement> statements = getIncomingStatements(res, predicate);
            DocumentModelList docs = new DocumentModelListImpl(statements.size());
            for (Statement st : statements) {
                DocumentModel dm = getDocumentModel(st.getSubject());
                if (dm != null) {
                    docs.add(dm);
                }
            }
            return docs;
        }
    }

    protected List<Statement> getIncomingStatements(QNameResource res, Resource predicate) {
        return relations.getGraphByName(getGraphName()).getStatements(null, predicate, res);
    }

    protected List<Statement> getOutgoingStatements(QNameResource res, Resource predicate) {
        return relations.getGraphByName(getGraphName()).getStatements(res, predicate, null);
    }

    protected DocumentModel getDocumentModel(Node node) {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Object> context = Collections.singletonMap(ResourceAdapter.CORE_SESSION_CONTEXT_KEY, session);
            Object o = relations.getResourceRepresentation(resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

    /**
     * @since 5.5
     */
    public String getGraphName() {
        if (StringUtils.isEmpty(graphName)) {
            return RelationConstants.GRAPH_NAME;
        }
        return graphName;
    }

}
