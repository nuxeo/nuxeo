/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     looping
 */
package org.nuxeo.ecm.platform.relations.services;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.relations.api.DocumentRelationManager;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.event.RelationEvents;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.2
 */
public class DocumentRelationService implements DocumentRelationManager {

    private RelationManager relationManager = null;

    protected RelationManager getRelationManager() {
        if (relationManager == null) {
            relationManager = Framework.getService(RelationManager.class);
        }
        return relationManager;
    }

    // for consistency for callers only
    private static void putStatements(Map<String, Serializable> options, List<Statement> statements) {
        options.put(RelationEvents.STATEMENTS_EVENT_KEY, (Serializable) statements);
    }

    private static void putStatements(Map<String, Serializable> options, Statement statement) {
        List<Statement> statements = new LinkedList<>();
        statements.add(statement);
        options.put(RelationEvents.STATEMENTS_EVENT_KEY, (Serializable) statements);
    }

    private QNameResource getNodeFromDocumentModel(DocumentModel model) {
        return (QNameResource) getRelationManager().getResource(RelationConstants.DOCUMENT_NAMESPACE, model, null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from, DocumentModel to, String predicate, boolean inverse)
            {
        addRelation(session, from, getNodeFromDocumentModel(to), predicate, inverse);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from, Node to, String predicate) {
        addRelation(session, from, to, predicate, false);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from, Node to, String predicate, boolean inverse)
            {
        addRelation(session, from, to, predicate, inverse, false);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from, Node to, String predicate, boolean inverse,
            boolean includeStatementsInEvents) {
        addRelation(session, from, to, predicate, inverse, includeStatementsInEvents, null);
    }

    @Override
    public void addRelation(CoreSession session, DocumentModel from, Node toResource, String predicate,
            boolean inverse, boolean includeStatementsInEvents, String comment) {
        Graph graph = getRelationManager().getGraph(RelationConstants.GRAPH_NAME, session);
        QNameResource fromResource = getNodeFromDocumentModel(from);

        Resource predicateResource = new ResourceImpl(predicate);
        Statement stmt;
        List<Statement> statements;
        if (inverse) {
            stmt = new StatementImpl(toResource, predicateResource, fromResource);
            statements = graph.getStatements(toResource, predicateResource, fromResource);
            if (statements != null && statements.size() > 0) {
                throw new RelationAlreadyExistsException();
            }
        } else {
            stmt = new StatementImpl(fromResource, predicateResource, toResource);
            statements = graph.getStatements(fromResource, predicateResource, toResource);
            if (statements != null && statements.size() > 0) {
                throw new RelationAlreadyExistsException();
            }
        }

        // Comment ?
        if (!StringUtils.isEmpty(comment)) {
            stmt.addProperty(RelationConstants.COMMENT, new LiteralImpl(comment));
        }
        Literal now = RelationDate.getLiteralDate(new Date());
        if (stmt.getProperties(RelationConstants.CREATION_DATE) == null) {
            stmt.addProperty(RelationConstants.CREATION_DATE, now);
        }
        if (stmt.getProperties(RelationConstants.MODIFICATION_DATE) == null) {
            stmt.addProperty(RelationConstants.MODIFICATION_DATE, now);
        }

        if (session.getPrincipal() != null && stmt.getProperty(RelationConstants.AUTHOR) == null) {
            stmt.addProperty(RelationConstants.AUTHOR, new LiteralImpl(session.getPrincipal().getName()));
        }

        // notifications

        Map<String, Serializable> options = new HashMap<>();
        String currentLifeCycleState = from.getCurrentLifeCycleState();
        options.put(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);
        if (includeStatementsInEvents) {
            putStatements(options, stmt);
        }
        options.put(RelationEvents.GRAPH_NAME_EVENT_KEY, RelationConstants.GRAPH_NAME);

        // before notification
        notifyEvent(RelationEvents.BEFORE_RELATION_CREATION, from, options, comment, session);

        // add statement
        graph.add(stmt);

        // XXX AT: try to refetch it from the graph so that resources are
        // transformed into qname resources: useful for indexing
        if (includeStatementsInEvents) {
            putStatements(options, graph.getStatements(stmt));
        }

        // after notification
        notifyEvent(RelationEvents.AFTER_RELATION_CREATION, from, options, comment, session);
    }

    protected void notifyEvent(String eventId, DocumentModel source, Map<String, Serializable> options, String comment,
            CoreSession session) {
        DocumentEventContext docCtx = new DocumentEventContext(session, session.getPrincipal(), source);
        options.put("category", RelationEvents.CATEGORY);
        options.put("comment", comment);

        EventProducer evtProducer = Framework.getService(EventProducer.class);
        evtProducer.fireEvent(docCtx.newEvent(eventId));
    }

    @Override
    public void deleteRelation(CoreSession session, DocumentModel from, DocumentModel to, String predicate)
            {
        deleteRelation(session, from, to, predicate, false);
    }

    @Override
    public void deleteRelation(CoreSession session, DocumentModel from, DocumentModel to, String predicate,
            boolean includeStatementsInEvents) {
        QNameResource fromResource = (QNameResource) getRelationManager().getResource(
                RelationConstants.DOCUMENT_NAMESPACE, from, null);
        QNameResource toResource = (QNameResource) getRelationManager().getResource(
                RelationConstants.DOCUMENT_NAMESPACE, to, null);
        Resource predicateResource = new ResourceImpl(predicate);
        Graph graph = getRelationManager().getGraphByName(RelationConstants.GRAPH_NAME);
        List<Statement> statements = graph.getStatements(fromResource, predicateResource, toResource);
        if (statements == null || statements.size() == 0) {
            // Silent ignore the deletion as it doesn't exist
            return;
        }
        for (Statement stmt : statements) {
            deleteRelation(session, stmt);
        }
    }

    @Override
    public void deleteRelation(CoreSession session, Statement stmt) {
        deleteRelation(session, stmt, false);
    }

    @Override
    public void deleteRelation(CoreSession session, Statement stmt, boolean includeStatementsInEvents)
            {

        // notifications
        Map<String, Serializable> options = new HashMap<>();

        // Find relative document
        DocumentModel eventDocument = null;
        if (stmt.getSubject() instanceof QNameResource) {
            eventDocument = (DocumentModel) getRelationManager().getResourceRepresentation(
                    RelationConstants.DOCUMENT_NAMESPACE, (QNameResource) stmt.getSubject(), null);
        } else if (stmt.getObject() instanceof QNameResource) {
            eventDocument = (DocumentModel) getRelationManager().getResourceRepresentation(
                    RelationConstants.DOCUMENT_NAMESPACE, (QNameResource) stmt.getObject(), null);
        }

        // Complete event info and send first event
        if (eventDocument != null) {
            String currentLifeCycleState = eventDocument.getCurrentLifeCycleState();
            options.put(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);
            options.put(RelationEvents.GRAPH_NAME_EVENT_KEY, RelationConstants.GRAPH_NAME);
            if (includeStatementsInEvents) {
                putStatements(options, stmt);
            }

            // before notification
            notifyEvent(RelationEvents.BEFORE_RELATION_REMOVAL, eventDocument, options, null, session);
        }

        // remove statement
        getRelationManager().getGraphByName(RelationConstants.GRAPH_NAME).remove(stmt);

        if (eventDocument != null) {
            // after notification
            notifyEvent(RelationEvents.AFTER_RELATION_REMOVAL, eventDocument, options, null, session);
        }
    }
}
