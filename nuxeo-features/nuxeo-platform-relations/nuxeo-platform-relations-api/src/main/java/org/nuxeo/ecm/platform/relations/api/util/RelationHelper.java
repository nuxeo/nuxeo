/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Eugen Ionica
 */

package org.nuxeo.ecm.platform.relations.api.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.*;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelationHelper {

    static RelationManager relationManager;

    private static final Log log = LogFactory.getLog(RelationHelper.class);

    // Utility class.
    private RelationHelper() {
    }

    public static RelationManager getRelationManager() {
        try {
            return Framework.getService(RelationManager.class);
        } catch (Exception e) {
            log.error(e, e);
        }
        return null;
    }

    /**
     * Returns the relation node corresponding to a document model.
     */
    public static QNameResource getDocumentResource(DocumentModel document)
            throws ClientException {
        QNameResource documentResource = null;
        RelationManager rm = getRelationManager();
        if (document != null && rm != null) {
            documentResource = (QNameResource) rm.getResource(
                    RelationConstants.DOCUMENT_NAMESPACE, document, null);
        }
        return documentResource;
    }

    /**
     * Returns the document model corresponding to a relation node.
     */
    public static DocumentModel getDocumentModel(Node node, String coreSessionId)
            throws ClientException {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put(ResourceAdapter.CORE_SESSION_ID_CONTEXT_KEY,
                    coreSessionId);
            Object o = getRelationManager().getResourceRepresentation(
                    resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

    public static DocumentModelList getSubjectDocuments(Resource predicat,
            String stringObject, String sessionId) {
        return getSubjectDocuments(RelationConstants.GRAPH_NAME, predicat, stringObject, sessionId);
    }

    public static DocumentModelList getSubjectDocuments(String graphName, Resource predicat,
            String stringObject, String sessionId) {
        try {
            Literal literal = new LiteralImpl(stringObject);
            Statement pattern = new StatementImpl(null, predicat, literal);
            List<Statement> stmts = getRelationManager().getStatements(
                    graphName, pattern);
            if (stmts != null) {
                DocumentModelList docs = new DocumentModelListImpl();
                for (Statement stmt : stmts) {
                    DocumentModel d = getDocumentModel(stmt.getSubject(),
                            sessionId);
                    if (d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public static DocumentModelList getSubjectDocuments(Resource predicat,
            DocumentModel objectDocument) {
        return getSubjectDocuments(RelationConstants.GRAPH_NAME, predicat, objectDocument);
    }

    public static DocumentModelList getSubjectDocuments(String graphName, Resource predicat,
            DocumentModel objectDocument) {
        try {
            QNameResource docResource = getDocumentResource(objectDocument);
            Statement pattern = new StatementImpl(null, predicat, docResource);
            List<Statement> stmts = getRelationManager().getStatements(
                    graphName, pattern);
            if (stmts != null) {
                DocumentModelList docs = new DocumentModelListImpl();
                for (Statement stmt : stmts) {
                    DocumentModel d = getDocumentModel(stmt.getSubject(),
                            objectDocument.getSessionId());
                    if (d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public static DocumentModelList getSubjectDocumentsOut(Resource predicat,
            DocumentModel objectDocument) {
        return getSubjectDocumentsOut(RelationConstants.GRAPH_NAME, predicat, objectDocument);
    }

    public static DocumentModelList getSubjectDocumentsOut(String graphName, Resource predicat,
            DocumentModel objectDocument) {
        try {
            QNameResource docResource = getDocumentResource(objectDocument);
            Statement pattern = new StatementImpl(docResource, predicat, null);
            List<Statement> stmts = getRelationManager().getStatements(
                    graphName, pattern);
            if (stmts != null) {
                DocumentModelList docs = new DocumentModelListImpl();
                for (Statement stmt : stmts) {
                    DocumentModel d = getDocumentModel(stmt.getObject(),
                            objectDocument.getSessionId());
                    if (d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public static DocumentModelList getObjectDocuments(
            DocumentModel subjectDoc, Resource predicat) {
        return getObjectDocuments(RelationConstants.GRAPH_NAME, subjectDoc, predicat);
    }

    public static DocumentModelList getObjectDocuments(String graphName,
            DocumentModel subjectDoc, Resource predicat) {
        try {
            List<Statement> stmts = getStatements(graphName, subjectDoc, predicat);
            if (stmts != null) {
                DocumentModelList docs = new DocumentModelListImpl();
                for (Statement stmt : stmts) {
                    DocumentModel d = getDocumentModel(stmt.getObject(),
                            subjectDoc.getSessionId());
                    if (d != null) {
                        docs.add(d);
                    }
                }
                return docs;
            }
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public static List<Statement> getStatements(DocumentModel subjectDoc,
            Resource predicat) {
        return getStatements(RelationConstants.GRAPH_NAME, subjectDoc, predicat);
    }

    public static List<Statement> getStatements(String graphName, DocumentModel subjectDoc,
            Resource predicat) {
        try {
            QNameResource docResource = getDocumentResource(subjectDoc);
            Statement pattern = new StatementImpl(docResource, predicat, null);
            return getRelationManager().getStatements(
                    graphName, pattern);
        } catch (ClientException e) {
            log.error(e, e);
        }
        return null;
    }

    public static void removeRelation(DocumentModel subjectDoc,
            Resource predicat, DocumentModel objectDoc) {
        removeRelation(RelationConstants.GRAPH_NAME, subjectDoc, predicat, objectDoc);
    }

    public static void removeRelation(String graphName, DocumentModel subjectDoc,
            Resource predicat, DocumentModel objectDoc) {
        try {
            QNameResource subject = getDocumentResource(subjectDoc);
            QNameResource object = getDocumentResource(objectDoc);
            List<Statement> stmts = new ArrayList<Statement>();
            Statement stmt = new StatementImpl(subject, predicat, object);
            stmts.add(stmt);
            getRelationManager().remove(graphName, stmts);
        } catch (ClientException e) {
            log.error(e, e);
        }
    }

}
