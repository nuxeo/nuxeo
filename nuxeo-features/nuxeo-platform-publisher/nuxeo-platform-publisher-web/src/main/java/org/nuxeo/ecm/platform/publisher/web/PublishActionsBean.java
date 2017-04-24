/*
 * (C) Copyright 2007-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Narcis Paslaru
 *     Florent Guillaume
 *     Thierry Martins
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.publisher.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublicationTreeNotAvailable;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.api.PublishingEvent;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * This Seam bean manages the publishing tab.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("publishActions")
@Scope(ScopeType.CONVERSATION)
public class PublishActionsBean extends AbstractPublishActions implements Serializable {

    public static class PublicationTreeInformation {

        private final String name;

        private final String title;

        public PublicationTreeInformation(String treeName, String treeTitle) {
            this.name = treeName;
            this.title = treeTitle;
        }

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }
    }

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PublishActionsBean.class);

    /**
     * @since 7.3
     */
    public static final List<String> TREE_TYPES_TO_FILTER = Arrays.asList("RootSectionsPublicationTree", "RenditionPublicationCoreTree");

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    protected transient PublisherService publisherService;

    protected String currentPublicationTreeNameForPublishing;

    protected PublicationTree currentPublicationTree;

    protected String publishingComment;

    protected static Set<String> sectionTypes;

    protected Map<String, String> publicationParameters = new HashMap<>();

    protected String treeSID;

    @Create
    public void create() {
        publisherService = Framework.getService(PublisherService.class);
    }

    @Destroy
    public void destroy() {
        if (currentPublicationTree != null) {
            currentPublicationTree.release();
            currentPublicationTree = null;
        }
        if (treeSID != null) {
            publisherService.releaseAllTrees(treeSID);
        }
    }

    protected Map<String, String> filterEmptyTrees(Map<String, String> trees) throws PublicationTreeNotAvailable {

        Map<String, String> filteredTrees = new HashMap<>();

        List<String> prefilteredTrees = filterEmptyTrees(trees.keySet());

        for (String ptree : prefilteredTrees) {
            filteredTrees.put(ptree, trees.get(ptree));
        }

        return filteredTrees;
    }

    protected List<String> filterEmptyTrees(Collection<String> trees) throws PublicationTreeNotAvailable {
        List<String> filteredTrees = new ArrayList<>();

        for (String tree : trees) {
            try {
                PublicationTree pTree = publisherService.getPublicationTree(tree, documentManager, null,
                        navigationContext.getCurrentDocument());
                if (pTree != null) {
                    if (TREE_TYPES_TO_FILTER.contains(pTree.getTreeType())) {
                        if (pTree.getChildrenNodes().size() > 0) {
                            filteredTrees.add(tree);
                        }
                    } else {
                        filteredTrees.add(tree);
                    }
                }
            } catch (PublicationTreeNotAvailable e) {
                log.warn("Publication tree " + tree + " is not available : check config");
                log.debug("Publication tree " + tree + " is not available : root cause is ", e);
            }
        }
        return filteredTrees;
    }

    @Factory(value = "availablePublicationTrees", scope = ScopeType.EVENT)
    public List<PublicationTreeInformation> getAvailablePublicationTrees() {
        Map<String, String> trees = publisherService.getAvailablePublicationTrees();
        // remove empty trees
        trees = filterEmptyTrees(trees);
        List<PublicationTreeInformation> treesInformation = new ArrayList<>();
        for (Map.Entry<String, String> entry : trees.entrySet()) {
            treesInformation.add(new PublicationTreeInformation(entry.getKey(), entry.getValue()));
        }
        return treesInformation;
    }

    public String doPublish(PublicationNode publicationNode) {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return doPublish(tree, publicationNode);
    }

    public String doPublish(PublicationTree tree, PublicationNode publicationNode) {
        if (tree == null) {
            return null;
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        PublishedDocument publishedDocument;
        try {
            publishedDocument = tree.publish(currentDocument, publicationNode, publicationParameters);
        } catch (NuxeoException e) {
            log.error(e, e);
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get(e.getMessage()));
            return null;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        if (publishedDocument.isPending()) {
            String comment = ComponentUtils.translate(context, "publishing.waiting", publicationNode.getPath(),
                    tree.getConfigName());
            // Log event on live version
            notifyEvent(PublishingEvent.documentWaitingPublication.name(), null, comment, null, currentDocument);
            Events.instance().raiseEvent(EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION);
            facesMessages.add(StatusMessage.Severity.INFO, messages.get("document_submitted_for_publication"),
                    messages.get(currentDocument.getType()));
        } else {
            String comment = ComponentUtils.translate(context, "publishing.done", publicationNode.getPath(),
                    tree.getConfigName());
            // Log event on live version
            notifyEvent(PublishingEvent.documentPublished.name(), null, comment, null, currentDocument);
            Events.instance().raiseEvent(EventNames.DOCUMENT_PUBLISHED);
            // publish may checkin the document -> change
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);
            facesMessages.add(StatusMessage.Severity.INFO, messages.get("document_published"),
                    messages.get(currentDocument.getType()));
        }
        navigationContext.invalidateCurrentDocument();
        currentPublicationTree = null;
        resetCache();
        return null;
    }

    /**
     * @since 5.9.3
     */
    protected void resetCache() {
        Contexts.getEventContext().remove("availablePublicationTrees");
        Contexts.getEventContext().remove("publishedDocuments");
    }

    public void setCurrentPublicationTreeNameForPublishing(String currentPublicationTreeNameForPublishing)
            {
        this.currentPublicationTreeNameForPublishing = currentPublicationTreeNameForPublishing;
        if (currentPublicationTree != null) {
            currentPublicationTree.release();
            currentPublicationTree = null;
        }
        currentPublicationTree = getCurrentPublicationTreeForPublishing();
    }

    public String getCurrentPublicationTreeNameForPublishing() {
        if (currentPublicationTreeNameForPublishing == null) {
            List<String> publicationTrees = new ArrayList<>(publisherService.getAvailablePublicationTree());
            publicationTrees = filterEmptyTrees(publicationTrees);
            if (!publicationTrees.isEmpty()) {
                currentPublicationTreeNameForPublishing = publicationTrees.get(0);
            }
        }
        return currentPublicationTreeNameForPublishing;
    }

    /**
     * Returns a list of publication trees.
     * <p>
     * Needed on top of {@link #getCurrentPublicationTreeForPublishing()} because RichFaces tree now requires roots to
     * be a list.
     *
     * @since 6.0
     */
    public List<PublicationTree> getCurrentPublicationTreesForPublishing() {
        List<PublicationTree> trees = new ArrayList<>();
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        if (tree != null) {
            trees.add(tree);
        }
        return trees;
    }

    public PublicationTree getCurrentPublicationTreeForPublishing() {
        if (currentPublicationTree == null) {
            if (getCurrentPublicationTreeNameForPublishing() == null) {
                return currentPublicationTree;
            }
            try {
                treeSID = documentManager.getSessionId();
                currentPublicationTree = publisherService.getPublicationTree(currentPublicationTreeNameForPublishing,
                        documentManager, null, navigationContext.getCurrentDocument());
            } catch (PublicationTreeNotAvailable e) {
                currentPublicationTree = null;
            }
        }
        return currentPublicationTree;
    }

    public String getCurrentPublicationTreeIconExpanded() {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return tree != null ? tree.getIconExpanded() : "";
    }

    public String getCurrentPublicationTreeIconCollapsed() {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return tree != null ? tree.getIconCollapsed() : "";
    }

    @Factory(value = "publishedDocuments", scope = ScopeType.EVENT)
    public List<PublishedDocument> getPublishedDocuments() {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        if (tree == null) {
            return Collections.emptyList();
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return tree.getExistingPublishedDocument(new DocumentLocationImpl(currentDocument));
    }

    public List<PublishedDocument> getPublishedDocumentsFor(String treeName) {
        if (treeName == null || "".equals(treeName)) {
            return null;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        try {
            PublicationTree tree = publisherService.getPublicationTree(treeName, documentManager, null);
            return tree.getExistingPublishedDocument(new DocumentLocationImpl(currentDocument));
        } catch (PublicationTreeNotAvailable e) {
            return null;
        }
    }

    public String unPublish(PublishedDocument publishedDocument) {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        if (tree != null) {
            tree.unpublish(publishedDocument);
        }
        // raise event without the container document as user may not have read
        // rights on it
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED);
        resetCache();
        return null;
    }

    public String rePublish(PublishedDocument publishedDocument) {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        if (tree == null) {
            log.error("Publication tree is null - cannot republish");
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get("error.document_republished"));
            return null;
        }
        PublicationNode node = tree.getNodeByPath(publishedDocument.getParentPath());
        return doPublish(tree, node);
    }

    public boolean canPublishTo(PublicationNode publicationNode) {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null || documentManager.getLockInfo(doc.getRef()) != null) {
            return false;
        }
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return tree != null ? tree.canPublishTo(publicationNode) : false;
    }

    public boolean canUnpublish(PublishedDocument publishedDocument) {
        PublicationTree tree = getCurrentPublicationTreeForPublishing();
        return tree != null ? tree.canUnpublish(publishedDocument) : false;
    }

    public boolean canRepublish(PublishedDocument publishedDocument) {
        if (!canUnpublish(publishedDocument)) {
            return false;
        }
        DocumentModel doc = navigationContext.getCurrentDocument();
        // version label is different, what means it is a previous version
        if (!publishedDocument.getSourceVersionLabel().equals(doc.getVersionLabel())) {
            return true;
        }
        // in case it is the same version, we have to check if the current
        // document has been modified since last publishing
        if (doc.isDirty()) {
            return true;
        }
        return false;
    }

    public boolean isPublishedDocument() {
        return publisherService.isPublishedDocument(navigationContext.getCurrentDocument());
    }

    public boolean canManagePublishing() {
        PublicationTree tree = publisherService.getPublicationTreeFor(navigationContext.getCurrentDocument(),
                documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(navigationContext.getCurrentDocument());
        return tree.canManagePublishing(publishedDocument);
    }

    public boolean hasValidationTask() {
        PublicationTree tree = publisherService.getPublicationTreeFor(navigationContext.getCurrentDocument(),
                documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(navigationContext.getCurrentDocument());
        return tree.hasValidationTask(publishedDocument);
    }

    public boolean isPending() {
        PublicationTree tree = publisherService.getPublicationTreeFor(navigationContext.getCurrentDocument(),
                documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(navigationContext.getCurrentDocument());
        return publishedDocument.isPending();
    }

    public String getPublishingComment() {
        return publishingComment;
    }

    public void setPublishingComment(String publishingComment) {
        this.publishingComment = publishingComment;
    }

    public class ApproverWithoutRestriction extends UnrestrictedSessionRunner {

        public DocumentModel sourceDocument;

        public DocumentModel liveDocument;

        public String comment;

        public PublishedDocument doc;

        public ApproverWithoutRestriction(PublishedDocument doc, String comment, CoreSession session) {
            super(session);
            this.doc = doc;
            this.comment = comment;
        }

        @Override
        public void run() {
            sourceDocument = session.getDocument(doc.getSourceDocumentRef());

            // soft dependency on Rendition system
            if (sourceDocument.hasFacet("Rendition")) {
                String uid = (String) sourceDocument.getPropertyValue("rend:sourceId");
                liveDocument = session.getDocument(new IdRef(uid));
            } else {
                liveDocument = session.getSourceDocument(sourceDocument.getRef());
            }
            sendApprovalEventToSourceDocument(session, sourceDocument, liveDocument, comment);

        }

        protected void sendApprovalEventToSourceDocument(CoreSession session, DocumentModel sourceDocument,
                DocumentModel liveVersion, String comment) {

            notifyEvent(session, PublishingEvent.documentPublicationApproved.name(), null, comment, null,
                    sourceDocument);

            if (!sourceDocument.getRef().equals(liveVersion.getRef())) {
                notifyEvent(session, PublishingEvent.documentPublicationApproved.name(), null, comment, null,
                        liveVersion);
            }
        }

    }

    public String approveDocument() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        PublicationTree tree = publisherService.getPublicationTreeFor(currentDocument, documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(currentDocument);
        tree.validatorPublishDocument(publishedDocument, publishingComment);

        FacesContext context = FacesContext.getCurrentInstance();
        String comment = publishingComment != null && publishingComment.length() > 0 ? ComponentUtils.translate(
                context, "publishing.approved.with.comment", publishedDocument.getParentPath(), tree.getConfigName(),
                publishingComment) : ComponentUtils.translate(context, "publishing.approved.without.comment",
                publishedDocument.getParentPath(), tree.getConfigName());

        ApproverWithoutRestriction approver = new ApproverWithoutRestriction(publishedDocument, comment,
                documentManager);
        if (documentManager.hasPermission(publishedDocument.getSourceDocumentRef(), SecurityConstants.WRITE)) {
            approver.run();
        } else {
            approver.runUnrestricted();
        }

        Events.instance().raiseEvent(EventNames.DOCUMENT_PUBLISHED);
        Events.instance().raiseEvent(EventNames.DOCUMENT_PUBLICATION_APPROVED);
        return null;
    }

    public String rejectDocument() {
        if (publishingComment == null || "".equals(publishingComment)) {
            facesMessages.addToControl("publishingComment", StatusMessage.Severity.ERROR,
                    messages.get("label.publishing.reject.user.comment.mandatory"));
            return null;
        }

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        PublicationTree tree = publisherService.getPublicationTreeFor(currentDocument, documentManager);
        PublishedDocument publishedDocument = tree.wrapToPublishedDocument(currentDocument);
        tree.validatorRejectPublication(publishedDocument, publishingComment);

        FacesContext context = FacesContext.getCurrentInstance();
        String comment = publishingComment != null && publishingComment.length() > 0 ? ComponentUtils.translate(
                context, "publishing.rejected.with.comment", publishedDocument.getParentPath(), tree.getConfigName(),
                publishingComment) : ComponentUtils.translate(context, "publishing.rejected.without.comment",
                publishedDocument.getParentPath(), tree.getConfigName());
        RejectWithoutRestrictionRunner runner = new RejectWithoutRestrictionRunner(documentManager, publishedDocument,
                comment);

        if (documentManager.hasPermission(publishedDocument.getSourceDocumentRef(), SecurityConstants.READ)) {
            runner.run();
        } else {
            runner.runUnrestricted();
        }
        Events.instance().raiseEvent(EventNames.DOCUMENT_PUBLICATION_REJECTED);

        return navigationContext.navigateToRef(navigationContext.getCurrentDocument().getParentRef());
    }

    public void unpublishDocumentsFromCurrentSelection() {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION)) {
            unpublish(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION));
        } else {
            log.debug("No selectable Documents in context to process unpublish on...");
        }
        log.debug("Unpublish the selected document(s) ...");
    }

    protected void unpublish(List<DocumentModel> documentModels) {
        for (DocumentModel documentModel : documentModels) {
            PublicationTree tree = publisherService.getPublicationTreeFor(documentModel, documentManager);
            PublishedDocument publishedDocument = tree.wrapToPublishedDocument(documentModel);
            tree.unpublish(publishedDocument);
        }

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED);

        Object[] params = { documentModels.size() };
        // remove from the current selection list
        documentsListsManager.resetWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);
        facesMessages.add(StatusMessage.Severity.INFO, messages.get("n_unpublished_docs"), params);
    }

    public boolean isRemotePublishedDocument(PublishedDocument publishedDocument) {
        if (publishedDocument == null) {
            return false;
        }
        return publishedDocument.getType().equals(PublishedDocument.Type.REMOTE);
    }

    public boolean isFileSystemPublishedDocument(PublishedDocument publishedDocument) {
        if (publishedDocument == null) {
            return false;
        }
        return publishedDocument.getType().equals(PublishedDocument.Type.FILE_SYSTEM);
    }

    public boolean isLocalPublishedDocument(PublishedDocument publishedDocument) {
        if (publishedDocument == null) {
            return false;
        }
        return publishedDocument.getType().equals(PublishedDocument.Type.LOCAL);
    }

    public String publishWorkList() {
        return publishDocumentList(DocumentsListsManager.DEFAULT_WORKING_LIST);
    }

    public DocumentModel getDocumentModelFor(String path) {
        DocumentRef docRef = new PathRef(path);
        if (documentManager.exists(docRef) && hasReadRight(path)) {
            return documentManager.getDocument(docRef);
        }
        return null;
    }

    public boolean hasReadRight(String documentPath) {
        return documentManager.hasPermission(new PathRef(documentPath), SecurityConstants.READ);
    }

    public String getFormattedPath(String path) {
        DocumentModel docModel = getDocumentModelFor(path);
        return docModel != null ? getFormattedPath(docModel) : path;
    }

    public String publishDocumentList(String listName) {
        List<DocumentModel> docs2Publish = documentsListsManager.getWorkingList(listName);
        DocumentModel target = navigationContext.getCurrentDocument();

        if (!getSectionTypes().contains(target.getType())) {
            return null;
        }

        PublicationNode targetNode = publisherService.wrapToPublicationNode(target, documentManager);
        if (targetNode == null) {
            return null;
        }

        int nbPublishedDocs = 0;
        for (DocumentModel doc : docs2Publish) {
            if (!documentManager.hasPermission(doc.getRef(), SecurityConstants.READ_PROPERTIES)) {
                continue;
            }

            if (doc.isProxy()) {
                // TODO copy also copies security. just recreate a proxy.
                documentManager.copy(doc.getRef(), target.getRef(), doc.getName());
                nbPublishedDocs++;
            } else {
                if (doc.hasFacet(FacetNames.PUBLISHABLE)) {
                    publisherService.publish(doc, targetNode);
                    nbPublishedDocs++;
                } else {
                    log.info("Attempted to publish non-publishable document " + doc.getTitle());
                }
            }
        }

        Object[] params = { nbPublishedDocs };
        facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get("n_published_docs"), params);

        if (nbPublishedDocs < docs2Publish.size()) {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get("selection_contains_non_publishable_docs"));
        }

        EventManager.raiseEventsOnDocumentChildrenChange(target);
        return null;
    }

    public Set<String> getSectionTypes() {
        if (sectionTypes == null) {
            sectionTypes = getTypeNamesForFacet(FacetNames.PUBLISH_SPACE);
            if (sectionTypes == null) {
                sectionTypes = new HashSet<>();
            }
        }
        return sectionTypes;
    }

    protected static Set<String> getTypeNamesForFacet(String facetName) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Set<String> publishRoots = schemaManager.getDocumentTypeNamesForFacet(facetName);
        if (publishRoots == null || publishRoots.isEmpty()) {
            return null;
        }
        return publishRoots;
    }

    public Map<String, String> getPublicationParameters() {
        return publicationParameters;
    }

    public void notifyEvent(String eventId, Map<String, Serializable> properties, String comment, String category,
            DocumentModel dm) {
        notifyEvent(documentManager, eventId, properties, comment, category, dm);
    }

    public static void notifyEvent(CoreSession session, String eventId, Map<String, Serializable> properties,
            String comment, String category, DocumentModel dm) {

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (properties == null) {
            properties = new HashMap<>();
        }

        properties.put(CoreEventConstants.REPOSITORY_NAME, session.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID, session.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE, dm.getCurrentLifeCycleState());

        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), dm);

        ctx.setProperties(properties);
        ctx.setComment(comment);
        ctx.setCategory(category);

        EventProducer evtProducer = Framework.getService(EventProducer.class);
        Event event = ctx.newEvent(eventId);
        evtProducer.fireEvent(event);
    }

    public String getDomainName(String treeName) {
        try {
            PublicationTree tree = publisherService.getPublicationTree(treeName, documentManager, null);
            Map<String, String> parameters = publisherService.getParametersFor(tree.getConfigName());
            String domainName = parameters.get(PublisherService.DOMAIN_NAME_KEY);
            return domainName != null ? " (" + domainName + ")" : "";
        } catch (PublicationTreeNotAvailable e) {
            return "";
        }
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void documentChanged() {
        currentPublicationTreeNameForPublishing = null;
        currentPublicationTree = null;
        publishingComment = null;
    }

    /**
     * @since 9.2
     */
    public void reset() {
        navigationContext.invalidateCurrentDocument();
        currentPublicationTreeNameForPublishing = null;
        currentPublicationTree = null;
    }

    class RejectWithoutRestrictionRunner extends UnrestrictedSessionRunner {

        PublishedDocument publishedDocument;

        DocumentModel sourceDocument;

        DocumentModel liveDocument;

        String comment;

        DocumentModel liveVersion;

        public RejectWithoutRestrictionRunner(CoreSession session, PublishedDocument publishedDocument, String comment) {
            super(session);
            this.publishedDocument = publishedDocument;
            this.comment = comment;
        }

        @Override
        public void run() {
            sourceDocument = session.getDocument(publishedDocument.getSourceDocumentRef());
            String sourceId = sourceDocument.getSourceId();
            // source may be null if the version is placeless (rendition)
            liveVersion = sourceId == null ? null : session.getDocument(new IdRef(sourceId));
            notifyRejectToSourceDocument();
        }

        private void notifyRejectToSourceDocument() {
            notifyEvent(PublishingEvent.documentPublicationRejected.name(), null, comment, null, sourceDocument);
            if (liveVersion != null && !sourceDocument.getRef().equals(liveVersion.getRef())) {
                notifyEvent(PublishingEvent.documentPublicationRejected.name(), null, comment, null, liveVersion);
            }
        }
    }
}
