/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
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
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = GetRelations.ID, category = Constants.CAT_SERVICES, label = "Get Linked Documents", description = "Get the relations for the input document. The 'outgoing' parameter ca be used to specify whether outgoing or incoming relations should be returned. Retuns a document list.")
public class GetRelations {

    public static final String ID = "Relations.GetRelations";

    @Context
    protected CoreSession session;

    @Context
    protected RelationManager relations;

    @Param(name = "predicate")
    // TODO use a combo box?
    protected String predicate;

    @Param(name = "outgoing", required = false)
    protected boolean outgoing = true;

    @OperationMethod
    public DocumentModelList run(DocumentModel doc) throws Exception {
        QNameResource res = getDocumentResource(doc);
        Resource predicate = getPredicate();
        return getDocuments(res, predicate);
    }

    protected QNameResource getDocumentResource(DocumentModel document)
            throws ClientException {
        return (QNameResource) relations.getResource(
                RelationConstants.DOCUMENT_NAMESPACE, document, null);
    }

    protected Resource getPredicate() {
        return predicate != null && predicate.length() > 0 ? new ResourceImpl(
                predicate) : null;
    }

    protected DocumentModelList getDocuments(QNameResource res,
            Resource predicate) throws ClientException {
        if (outgoing) {
            List<Statement> statements = getOutgoingStatements(res, predicate);
            DocumentModelList docs = new DocumentModelListImpl(
                    statements.size());
            for (Statement st : statements) {
                DocumentModel dm = getDocumentModel(st.getObject());
                if (dm != null) {
                    docs.add(dm);
                }
            }
            return docs;
        } else {
            List<Statement> statements = getIncomingStatements(res, predicate);
            DocumentModelList docs = new DocumentModelListImpl(
                    statements.size());
            for (Statement st : statements) {
                DocumentModel dm = getDocumentModel(st.getSubject());
                if (dm != null) {
                    docs.add(dm);
                }
            }
            return docs;
        }
    }

    protected List<Statement> getIncomingStatements(QNameResource res,
            Resource predicate) throws ClientException {
        Statement pattern = new StatementImpl(null, predicate, res);
        return relations.getStatements(RelationConstants.GRAPH_NAME, pattern);
    }

    protected List<Statement> getOutgoingStatements(QNameResource res,
            Resource predicate) throws ClientException {
        Statement pattern = new StatementImpl(res, predicate, null);
        return relations.getStatements(RelationConstants.GRAPH_NAME, pattern);
    }

    protected DocumentModel getDocumentModel(Node node) throws ClientException {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put(ResourceAdapter.CORE_SESSION_ID_CONTEXT_KEY,
                    session.getSessionId());
            Object o = relations.getResourceRepresentation(
                    resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

}
