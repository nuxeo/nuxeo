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
 * $Id: RelationActionsBean.java 28951 2008-01-11 13:35:15Z tdelprat $
 */

package org.nuxeo.ecm.platform.relations.web.listener.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.DocumentRelationManager;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.jena.JenaGraph;
import org.nuxeo.ecm.platform.relations.web.NodeInfo;
import org.nuxeo.ecm.platform.relations.web.NodeInfoImpl;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;
import org.nuxeo.ecm.platform.relations.web.StatementInfoComparator;
import org.nuxeo.ecm.platform.relations.web.StatementInfoImpl;
import org.nuxeo.ecm.platform.relations.web.listener.RelationActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.invalidations.AutomaticDocumentBasedInvalidation;
import org.nuxeo.ecm.platform.ui.web.invalidations.DocumentContextBoundActionBean;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Seam component that manages statements involving current document as well as creation, edition and deletion of
 * statements involving current document.
 * <p>
 * Current document is the subject of the relation. The predicate is resolved thanks to a list of predicates URIs. The
 * object is resolved using a type (literal, resource, qname resource), an optional namespace (for qname resources) and
 * a value.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@Name("relationActions")
@Scope(CONVERSATION)
@AutomaticDocumentBasedInvalidation
public class RelationActionsBean extends DocumentContextBoundActionBean implements RelationActions, Serializable {

    private static final long serialVersionUID = 2336539966097558178L;

    private static final Log log = LogFactory.getLog(RelationActionsBean.class);

    protected static boolean includeStatementsInEvents = false;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected RelationManager relationManager;

    @In(create = true)
    protected DocumentRelationManager documentRelationManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(required = false)
    protected transient Principal currentUser;

    // statements lists
    protected List<Statement> incomingStatements;

    protected List<StatementInfo> incomingStatementsInfo;

    protected List<Statement> outgoingStatements;

    protected List<StatementInfo> outgoingStatementsInfo;

    // fields for relation creation

    protected String predicateUri;

    protected String objectType;

    protected String objectLiteralValue;

    protected String objectUri;

    protected String objectDocumentUid;

    protected String objectDocumentTitle;

    protected String comment;

    protected Boolean showCreateForm = false;

    // popupDisplayed flag for preventing relation_search content view execution
    // until search button clicked
    protected Boolean popupDisplayed = false;

    @Override
    public DocumentModel getDocumentModel(Node node) throws ClientException {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Object> context = Collections.<String, Object> singletonMap(
                    ResourceAdapter.CORE_SESSION_CONTEXT_KEY, documentManager);
            Object o = relationManager.getResourceRepresentation(resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

    // XXX AT: for BBB when repo name was not included in the resource uri
    @Deprecated
    private static QNameResource getOldDocumentResource(DocumentModel document) {
        QNameResource documentResource = null;
        if (document != null) {
            documentResource = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE, document.getId());
        }
        return documentResource;
    }

    @Override
    public QNameResource getDocumentResource(DocumentModel document) throws ClientException {
        QNameResource documentResource = null;
        if (document != null) {
            documentResource = (QNameResource) relationManager.getResource(RelationConstants.DOCUMENT_NAMESPACE,
                    document, null);
        }
        return documentResource;
    }

    protected List<StatementInfo> getStatementsInfo(List<Statement> statements) throws ClientException {
        if (statements == null) {
            return null;
        }
        List<StatementInfo> infoList = new ArrayList<StatementInfo>();
        for (Statement statement : statements) {
            Subject subject = statement.getSubject();
            // TODO: filter on doc visibility (?)
            NodeInfo subjectInfo = new NodeInfoImpl(subject, getDocumentModel(subject), true);
            Resource predicate = statement.getPredicate();
            Node object = statement.getObject();
            NodeInfo objectInfo = new NodeInfoImpl(object, getDocumentModel(object), true);
            StatementInfo info = new StatementInfoImpl(statement, subjectInfo, new NodeInfoImpl(predicate), objectInfo);
            infoList.add(info);
        }
        return infoList;
    }

    protected void resetEventContext() {
        Context evtCtx = Contexts.getEventContext();
        if (evtCtx != null) {
            evtCtx.remove("currentDocumentIncomingRelations");
            evtCtx.remove("currentDocumentOutgoingRelations");
        }
    }

    @Override
    @Factory(value = "currentDocumentIncomingRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getIncomingStatementsInfo() throws ClientException {
        if (incomingStatementsInfo != null) {
            return incomingStatementsInfo;
        }
        DocumentModel currentDoc = getCurrentDocument();
        Resource docResource = getDocumentResource(currentDoc);
        if (docResource == null) {
            incomingStatements = Collections.emptyList();
            incomingStatementsInfo = Collections.emptyList();
        } else {
            Graph graph = relationManager.getGraphByName(RelationConstants.GRAPH_NAME);
            incomingStatements = graph.getStatements(null, null, docResource);
            if (graph instanceof JenaGraph) {
                // add old statements, BBB
                Resource oldDocResource = getOldDocumentResource(currentDoc);
                incomingStatements.addAll(graph.getStatements(null, null, oldDocResource));
            }
            incomingStatementsInfo = getStatementsInfo(incomingStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(new StatementInfoComparator());
            Collections.sort(incomingStatementsInfo, comp);
        }
        return incomingStatementsInfo;
    }

    @Override
    @Factory(value = "currentDocumentOutgoingRelations", scope = ScopeType.EVENT)
    public List<StatementInfo> getOutgoingStatementsInfo() throws ClientException {
        if (outgoingStatementsInfo != null) {
            return outgoingStatementsInfo;
        }
        DocumentModel currentDoc = getCurrentDocument();
        Resource docResource = getDocumentResource(currentDoc);
        if (docResource == null) {
            outgoingStatements = Collections.emptyList();
            outgoingStatementsInfo = Collections.emptyList();
        } else {
            Graph graph = relationManager.getGraphByName(RelationConstants.GRAPH_NAME);
            outgoingStatements = graph.getStatements(docResource, null, null);
            if (graph instanceof JenaGraph) {
                // add old statements, BBB
                Resource oldDocResource = getOldDocumentResource(currentDoc);
                outgoingStatements.addAll(graph.getStatements(oldDocResource, null, null));
            }
            outgoingStatementsInfo = getStatementsInfo(outgoingStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(new StatementInfoComparator());
            Collections.sort(outgoingStatementsInfo, comp);
        }
        return outgoingStatementsInfo;
    }

    @Override
    public void resetStatements() {
        incomingStatements = null;
        incomingStatementsInfo = null;
        outgoingStatements = null;
        outgoingStatementsInfo = null;
    }

    // getters & setters for creation items

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getObjectDocumentTitle() {
        return objectDocumentTitle;
    }

    @Override
    public void setObjectDocumentTitle(String objectDocumentTitle) {
        this.objectDocumentTitle = objectDocumentTitle;
    }

    @Override
    public String getObjectDocumentUid() {
        return objectDocumentUid;
    }

    @Override
    public void setObjectDocumentUid(String objectDocumentUid) {
        this.objectDocumentUid = objectDocumentUid;
    }

    @Override
    public String getObjectLiteralValue() {
        return objectLiteralValue;
    }

    @Override
    public void setObjectLiteralValue(String objectLiteralValue) {
        this.objectLiteralValue = objectLiteralValue;
    }

    @Override
    public String getObjectType() {
        return objectType;
    }

    @Override
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    @Override
    public String getObjectUri() {
        return objectUri;
    }

    @Override
    public void setObjectUri(String objectUri) {
        this.objectUri = objectUri;
    }

    @Override
    public String getPredicateUri() {
        return predicateUri;
    }

    @Override
    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    @Override
    public String addStatement() throws ClientException {
        resetEventContext();

        Node object = null;
        if (objectType.equals("literal")) {
            objectLiteralValue = objectLiteralValue.trim();
            object = new LiteralImpl(objectLiteralValue);
        } else if (objectType.equals("uri")) {
            objectUri = objectUri.trim();
            object = new ResourceImpl(objectUri);
        } else if (objectType.equals("document")) {
            objectDocumentUid = objectDocumentUid.trim();
            String repositoryName = navigationContext.getCurrentServerLocation().getName();
            String localName = repositoryName + "/" + objectDocumentUid;
            object = new QNameResourceImpl(RelationConstants.DOCUMENT_NAMESPACE, localName);
        }
        try {
            documentRelationManager.addRelation(documentManager, getCurrentDocument(), object, predicateUri, false,
                    includeStatementsInEvents, StringUtils.trim(comment));
            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get("label.relation.created"));
            resetCreateFormValues();
        } catch (RelationAlreadyExistsException e) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    resourcesAccessor.getMessages().get("label.relation.already.exists"));
        }
        resetStatements();
        return null;
    }

    @Override
    public void toggleCreateForm(ActionEvent event) {
        showCreateForm = !showCreateForm;
    }

    private void resetCreateFormValues() {
        predicateUri = "";
        objectType = "";
        objectLiteralValue = "";
        objectUri = "";
        objectDocumentUid = "";
        objectDocumentTitle = "";
        comment = "";
        showCreateForm = false;
        popupDisplayed = false;
    }

    @Override
    public String deleteStatement(StatementInfo stmtInfo) throws ClientException {
        resetEventContext();
        documentRelationManager.deleteRelation(documentManager, stmtInfo.getStatement());
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get("label.relation.deleted"));
        resetStatements();
        return null;
    }

    @Override
    public Boolean getShowCreateForm() {
        return showCreateForm;
    }

    @Override
    protected void resetBeanCache(DocumentModel newCurrentDocumentModel) {
        resetStatements();
    }

    public Boolean getPopupDisplayed() {
        return popupDisplayed;
    }

    public void setPopupDisplayed(Boolean popupDisplayed) {
        this.popupDisplayed = popupDisplayed;
    }

}
