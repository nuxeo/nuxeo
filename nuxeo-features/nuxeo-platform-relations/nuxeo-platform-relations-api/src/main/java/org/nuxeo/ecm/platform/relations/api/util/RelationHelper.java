/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Eugen Ionica
 */

package org.nuxeo.ecm.platform.relations.api.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.api.Framework;

public class RelationHelper {

    static RelationManager relationManager;

    private static final Log log = LogFactory.getLog(RelationHelper.class);

    // Utility class.
    private RelationHelper() {
    }

    public static RelationManager getRelationManager() {
        return Framework.getService(RelationManager.class);
    }

    /**
     * Returns the relation node corresponding to a document model.
     */
    public static QNameResource getDocumentResource(DocumentModel document) {
        QNameResource documentResource = null;
        RelationManager rm = getRelationManager();
        if (document != null && rm != null) {
            documentResource = (QNameResource) rm.getResource(RelationConstants.DOCUMENT_NAMESPACE, document, null);
        }
        return documentResource;
    }

    /**
     * Returns the document model corresponding to a relation node.
     */
    public static DocumentModel getDocumentModel(Node node, CoreSession session) {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Object> context = Collections.<String, Object> singletonMap(
                    ResourceAdapter.CORE_SESSION_CONTEXT_KEY, session);
            Object o = getRelationManager().getResourceRepresentation(resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

    public static DocumentModelList getSubjectDocuments(Resource predicat, String stringObject, CoreSession session) {
        return getSubjectDocuments(RelationConstants.GRAPH_NAME, predicat, stringObject, session);
    }

    public static DocumentModelList getSubjectDocuments(String graphName, Resource predicat, String stringObject,
            CoreSession session) {
        Literal literal = new LiteralImpl(stringObject);
        List<Statement> stmts = getRelationManager().getGraphByName(graphName).getStatements(null, predicat, literal);
        if (stmts != null) {
            DocumentModelList docs = new DocumentModelListImpl();
            for (Statement stmt : stmts) {
                DocumentModel d = getDocumentModel(stmt.getSubject(), session);
                if (d != null) {
                    docs.add(d);
                }
            }
            return docs;
        }
        return null;
    }

    public static DocumentModelList getSubjectDocuments(Resource predicat, DocumentModel objectDocument) {
        return getSubjectDocuments(RelationConstants.GRAPH_NAME, predicat, objectDocument);
    }

    public static DocumentModelList getSubjectDocuments(String graphName, Resource predicat,
            DocumentModel objectDocument) {
        QNameResource docResource = getDocumentResource(objectDocument);
        List<Statement> stmts = getRelationManager().getGraphByName(graphName).getStatements(null, predicat,
                docResource);
        if (stmts != null) {
            DocumentModelList docs = new DocumentModelListImpl();
            for (Statement stmt : stmts) {
                DocumentModel d = getDocumentModel(stmt.getSubject(), objectDocument.getCoreSession());
                if (d != null) {
                    docs.add(d);
                }
            }
            return docs;
        }
        return null;
    }

    public static DocumentModelList getSubjectDocumentsOut(Resource predicat, DocumentModel objectDocument) {
        return getSubjectDocumentsOut(RelationConstants.GRAPH_NAME, predicat, objectDocument);
    }

    public static DocumentModelList getSubjectDocumentsOut(String graphName, Resource predicat,
            DocumentModel objectDocument) {
        QNameResource docResource = getDocumentResource(objectDocument);
        List<Statement> stmts = getRelationManager().getGraphByName(graphName).getStatements(docResource, predicat,
                null);
        if (stmts != null) {
            DocumentModelList docs = new DocumentModelListImpl();
            for (Statement stmt : stmts) {
                DocumentModel d = getDocumentModel(stmt.getObject(), objectDocument.getCoreSession());
                if (d != null) {
                    docs.add(d);
                }
            }
            return docs;
        }
        return null;
    }

    public static DocumentModelList getObjectDocuments(DocumentModel subjectDoc, Resource predicat) {
        return getObjectDocuments(RelationConstants.GRAPH_NAME, subjectDoc, predicat);
    }

    public static DocumentModelList getObjectDocuments(String graphName, DocumentModel subjectDoc, Resource predicat) {
        List<Statement> stmts = getStatements(graphName, subjectDoc, predicat);
        if (stmts != null) {
            DocumentModelList docs = new DocumentModelListImpl();
            for (Statement stmt : stmts) {
                DocumentModel d = getDocumentModel(stmt.getObject(), subjectDoc.getCoreSession());
                if (d != null) {
                    docs.add(d);
                }
            }
            return docs;
        }
        return null;
    }

    public static List<Statement> getStatements(DocumentModel subjectDoc, Resource predicat) {
        return getStatements(RelationConstants.GRAPH_NAME, subjectDoc, predicat);
    }

    public static List<Statement> getStatements(String graphName, DocumentModel subjectDoc, Resource predicat) {
        QNameResource docResource = getDocumentResource(subjectDoc);
        return getRelationManager().getGraphByName(graphName).getStatements(docResource, predicat, null);
    }

    public static void removeRelation(DocumentModel subjectDoc, Resource predicat, DocumentModel objectDoc) {
        removeRelation(RelationConstants.GRAPH_NAME, subjectDoc, predicat, objectDoc);
    }

    public static void removeRelation(String graphName, DocumentModel subjectDoc, Resource predicat,
            DocumentModel objectDoc) {
        QNameResource subject = getDocumentResource(subjectDoc);
        QNameResource object = getDocumentResource(objectDoc);
        Statement stmt = new StatementImpl(subject, predicat, object);
        getRelationManager().getGraphByName(graphName).remove(stmt);
    }

}
