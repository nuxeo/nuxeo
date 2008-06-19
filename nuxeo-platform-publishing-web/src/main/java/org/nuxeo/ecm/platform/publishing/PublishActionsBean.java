/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Narcis Paslaru
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.publishing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.WebRemote;
import org.jboss.seam.core.Events;
import org.jboss.seam.core.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModelTree;
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeNodeComparator;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeNodeImpl;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.TypeService;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.publishing.api.PublishActions;
import org.nuxeo.ecm.platform.publishing.api.PublishingInformation;
import org.nuxeo.ecm.platform.publishing.api.PublishingService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;
import org.nuxeo.runtime.api.Framework;

/**
 * This Seam bean manages the publishing tab.
 *
 * @author Narcis Paslaru
 * @author Florent Guillaume
 * @author Thierry Martins
 */

@Name("publishActions")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class PublishActionsBean implements PublishActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PublishActionsBean.class);

    private static final String PUBLISH_DOCUMENT = "PUBLISH_DOCUMENT";

    protected static final String DOMAIN_SECTIONS = "DOMAIN_SECTIONS";

    private static final String DOMAIN_TYPE = "Domain";

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient VersioningManager versioningManager;

    @In(create = true, required = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected transient PrincipalListManager principalListManager;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @RequestParameter
    private String unPublishSectionRef;

    // hierarchy of existing sections
    private transient SelectDataModel sectionsModel;

    // list of selected sections
    private List<DocumentModelTreeNode> selectedSections;

    // map to store for each sections the existing published proxies
    // XXX AT: see if there is a better way to retrieve/store this information
    // (see #getPublicationInformation)
    private Map<String, DocumentModel> existingPublishedProxy;

    private transient DocumentMessageProducer docMsgProducer;

    private transient PublishingService publishingService;

    private String comment;

    private static Set<String> sectionRootTypes;

    private static Set<String> sectionTypes;

    public Set<String> getSectionRootTypes() {
        if (sectionRootTypes == null) {
            sectionRootTypes = getTypeNamesForFacet("MasterPublishSpace");
            if (sectionRootTypes == null) {
                sectionRootTypes = new HashSet<String>();
                sectionRootTypes.add("SectionRoot");
            }
        }
        return sectionRootTypes;
    }

    public Set<String> getSectionTypes() {
        if (sectionTypes == null) {
            sectionTypes = getTypeNamesForFacet("PublishSpace");
            if (sectionTypes == null) {
                sectionTypes = new HashSet<String>();
                sectionTypes.add("Section");
            }
        }
        return sectionTypes;

    }

    private DocumentMessageProducer getDocumentMessageProducer()
            throws Exception {
        if (docMsgProducer == null) {
            docMsgProducer = DocumentMessageProducerBusinessDelegate.getRemoteDocumentMessageProducer();
        }
        return docMsgProducer;
    }

    /*
     * Called by publish_buttons.xhtml
     */
    public List<Action> getActionsForPublishDocument() {
        return webActions.getActionsList(PUBLISH_DOCUMENT);
    }

    private Set<String> getTypeNamesForFacet(String facetName) {
        TypeService schemaService;
        try {
            // XXX should use getService(SchemaManager.class)
            schemaService = (TypeService) Framework.getRuntime().getComponent(
                    TypeService.NAME);
        } catch (Exception e) {
            log.error("Exception in retrieving publish spaces : ", e);
            return null;
        }

        Set<String> publishRoots = schemaService.getTypeManager().getDocumentTypeNamesForFacet(
                facetName);
        if (publishRoots == null || publishRoots.isEmpty()) {
            return null;
        }
        return publishRoots;
    }

    // TODO move this class to nuxeo-core-api
    /**
     * Helper class to run something with an unrestricted session.
     * <p>
     * The caller should implement the run() method.
     *
     * @author Florent Guillaume
     */
    protected static abstract class UnrestrictedSessionRunner {

        protected String repositoryName;

        protected CoreSession unrestrictedSession;

        protected Object misc;

        public UnrestrictedSessionRunner(String repositoryName, Object misc)
                throws ClientException {
            this.repositoryName = repositoryName;
            this.misc = misc;
        }

        public void runUnrestricted() throws ClientException {
            LoginContext loginContext;
            try {
                loginContext = Framework.login();
            } catch (LoginException e) {
                throw new ClientException(e);
            }
            try {
                Repository repository;
                try {
                    RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
                    repository = repositoryManager.getRepository(repositoryName);
                } catch (ClientException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ClientException(e);
                }
                if (repository == null) {
                    throw new ClientException("Cannot get repository " +
                            repositoryName);
                }
                try {
                    unrestrictedSession = repository.open();
                } catch (ClientException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ClientException(e);
                }
                try {
                    /*
                     * Call user code.
                     */
                    run();
                } finally {
                    try {
                        repository.close(unrestrictedSession);
                    } catch (ClientException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new ClientException(e);
                    }
                }
            } finally {
                try {
                    loginContext.logout();
                } catch (LoginException e) {
                    throw new ClientException(e);
                }
            }
        }

        /**
         * This method will run with {@link #unrestrictedSession} available. The
         * field {@link #misc}, suitably cast, can be used to pass values back
         * to the caller.
         *
         * @throws ClientException
         */
        public abstract void run() throws ClientException;

    }

    protected void getSectionsSelectModel() throws ClientException {
        String repositoryName = navigationContext.getCurrentDocument().getRepositoryName();

        /*
         * Get the section list from an unrestricted session.
         */
        new UnrestrictedSessionRunner(repositoryName, null) {
            @Override
            public void run() throws ClientException {
                getSectionRootTypes();
                getSectionTypes();
                DocumentModelTree sections = new DocumentModelTreeImpl();
                DocumentModelList domains = unrestrictedSession.getChildren(
                        unrestrictedSession.getRootDocument().getRef(),
                        DOMAIN_TYPE);
                for (DocumentModel domain : domains) {
                    for (String sectionRootNameType : sectionRootTypes) {
                        DocumentModelList children = unrestrictedSession.getChildren(
                                domain.getRef(), sectionRootNameType);
                        for (DocumentModel sectionRoot : children) {
                            String sectionRootPath = sectionRoot.getPathAsString();
                            for (String sectionNameType : sectionTypes) {
                                accumulateAvailableSections(sections,
                                        sectionRootPath, sectionNameType);
                            }
                        }
                    }
                }

                // check for already selected sections
                DocumentModel currentDocument = navigationContext.getCurrentDocument();
                DocumentRef currentDocRef = currentDocument.getRef();
                DocumentRef currentParentRef = currentDocument.getParentRef();
                existingPublishedProxy = new HashMap<String, DocumentModel>();
                DocumentModelList publishedProxies = documentManager.getProxies(
                        currentDocRef, null);
                for (DocumentModel pProxy : publishedProxies) {
                    for (DocumentModelTreeNode node : sections) {
                        DocumentRef proxyParentRef = pProxy.getParentRef();
                        DocumentRef sectionRef = node.getDocument().getRef();
                        if (sectionRef.equals(proxyParentRef)) {
                            String versionLabel = versioningManager.getVersionLabel(pProxy);
                            node.setVersion(versionLabel);
                            existingPublishedProxy.put(proxyParentRef.toString(),
                                    pProxy);
                            if (!sectionRef.equals(currentParentRef)) {
                                // when looking at a proxy, don't check itself
                                addSelectedSection(node);
                            }
                            break;
                        }
                    }
                }

                SelectDataModel model = new SelectDataModelImpl(
                        SECTIONS_DOCUMENT_TREE, sections, getSelectedSections());
                model.addSelectModelListener(PublishActionsBean.this);
                // XXX AT: see if cache is useful
                // cacheUpdateNotifier.addCacheListener(model);
                setSectionsModel(model);

            }

        }.runUnrestricted();

    }

    private void accumulateAvailableSections(DocumentModelTree sections,
            String sectionRootPath, String sectionNameType)
            throws ClientException {

        Object[] params = new Object[] { sectionRootPath, sectionNameType };

        PagedDocumentsProvider sectionsProvider = null;
        try {
            sectionsProvider = queryModelActions.get(DOMAIN_SECTIONS).getResultsProvider(
                    params);
        } catch (QueryException e) {
            throw new ClientException(String.format("Invalid search query. "
                    + "Check the \"%s\" QueryModel configuration",
                    DOMAIN_SECTIONS), e);
        }
        sectionsProvider.rewind();
        DocumentModelList mainSections = sectionsProvider.getCurrentPage();

        while (sectionsProvider.isNextPageAvailable()) {
            mainSections.addAll(sectionsProvider.getNextPage());
        }

        int firstLevel = sectionRootPath.split("/").length + 1;

        DocumentModelTreeImpl nodes = new DocumentModelTreeImpl();
        for (DocumentModel currentSection : mainSections) {
            if (documentManager.hasPermission(currentSection.getRef(),
                    SecurityConstants.READ)) {
                int currentLevel = currentSection.getPathAsString().split("/").length;
                nodes.add(currentSection, currentLevel - firstLevel);
            }
        }
        // sort sections using titles
        DocumentModelTreeNodeComparator comp = new DocumentModelTreeNodeComparator(
                nodes.getPathTitles());
        Collections.sort((ArrayList)nodes, comp);

        // populate sections
        for (DocumentModelTreeNode node: nodes) {
            sections.add(node);
        }
    }

    // TODO move to protected
    public DocumentModelList getProxies(DocumentModel docModel)
            throws ClientException {
        if (docModel == null) {
            return null;
        } else {
            return documentManager.getProxies(docModel.getRef(), null);
        }
    }

    // TODO move to protected
    public List<PublishingInformation> getPublishingInformation(
            DocumentModel docModel) throws ClientException {
        DocumentModelList proxies = getProxies(docModel);
        if (proxies == null) {
            return null;
        }
        List<PublishingInformation> info = new ArrayList<PublishingInformation>(
                proxies.size());
        for (DocumentModel proxy : proxies) {
            DocumentRef parentRef = proxy.getParentRef();
            DocumentModel section = null;
            try {
                section = documentManager.getDocument(parentRef);
            } catch (ClientException e) {
            }
            info.add(new PublishingInformation(proxy, section));
        }
        return info;
    }

    /*
     * Called by action DOCUMENT_PUBLISH.
     */
    public String publishDocument() throws ClientException {
        DocumentModel docToPublish = navigationContext.getCurrentDocument();

        if (documentManager.getLock(docToPublish.getRef()) != null) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "error.document.locked.for.publish"));
            return null;
        }

        List<DocumentModelTreeNode> selectedSections = getSelectedSections();
        if (selectedSections.isEmpty()) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "publish.no_section_selected"));

            return null;
        }

        log.debug("selected " + selectedSections.size() + " sections");

        /**
         * Proxies for which we need a moderation. Let's request the moderation
         * after the document manager session has been saved to avoid conflicts
         * in between sync txn and async txn that can start before the end of
         * the sync txn.
         */
        List<DocumentModel> forModeration = new ArrayList<DocumentModel>();

        for (DocumentModelTreeNode section : selectedSections) {
            boolean moderation = !isAlreadyPublishedInSection(docToPublish,
                    section.getDocument());

            DocumentModel proxy = publishDocument(docToPublish,
                    section.getDocument());

            if (moderation && !isReviewer(proxy)) {
                forModeration.add(proxy);
            }
        }

        // A document is considered published if it doesn't have
        // approval from section's manager
        boolean published = false;
        if (selectedSections.size() > forModeration.size()) {
            published = true;
        }

        if (published) {

            // notifyEvent(org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLISHED,
            // null, comment,
            // null, docToPublish);

            comment = null;
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_published"),
                    resourcesAccessor.getMessages().get(docToPublish.getType()));
        }

        if (!forModeration.isEmpty()) {
            // notifyEvent(org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION,
            // null, comment,
            // null, docToPublish);
            comment = null;
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "document_submitted_for_publication"),
                    resourcesAccessor.getMessages().get(docToPublish.getType()));
            for (DocumentModel proxy : forModeration) {
                notifyEvent(
                        org.nuxeo.ecm.webapp.helpers.EventNames.PROXY_PUSLISHING_PENDING,
                        null, comment, null, proxy);
            }
        }

        setSectionsModel(null);

        return null;
    }

    public boolean isReviewer(DocumentModel dm) throws ClientException {
        // TODO: introduce a specific publication reviewer permission
        return documentManager.hasPermission(dm.getRef(),
                SecurityConstants.WRITE_PROPERTIES);
    }

    // TODO move to protected
    public boolean isAlreadyPublishedInSection(DocumentModel doc,
            DocumentModel section) throws ClientException {
        // XXX We need refetch here for the new one but we should add this to
        // the seam cache somewhere.
        for (PublishingInformation each : getPublishingInformation(doc)) {
            // FIXME do a better comparison using ref directly. Need to test a
            // bit more since it appears to fail.
            // Furthermore, current DocumentModel implementation performs remote
            // call to get the Path.
            if (each.getSection().getPathAsString().equals(
                    section.getPathAsString())) {
                return true;
            }
        }
        return false;
    }

    /*
     * Called by action WORKLIST_PUBLISH.
     */
    public String publishWorkList() throws ClientException {
        return publishDocumentList(DocumentsListsManager.DEFAULT_WORKING_LIST);
    }

    public String publishDocumentList(String listName) throws ClientException {

        List<DocumentModel> docs2Publish = documentsListsManager.getWorkingList(listName);
        DocumentModel target = navigationContext.getCurrentDocument();

        if (!getSectionTypes().contains(target.getType())) {
            return null;
        }

        int nbPublishedDocs = 0;
        for (DocumentModel doc : docs2Publish) {
            if (!documentManager.hasPermission(doc.getRef(),
                    SecurityConstants.READ_PROPERTIES)) {
                continue;
            }

            if (doc.isProxy()) {
                // TODO copy also copies security. just recreate a proxy.
                documentManager.copy(doc.getRef(), target.getRef(),
                        doc.getName());
                nbPublishedDocs++;
            } else {
                if (doc.hasFacet(FacetNames.PUBLISHABLE)) {
                    documentManager.publishDocument(doc, target);
                    nbPublishedDocs++;
                } else {
                    log.info("Attempted to publish non-publishable document " +
                            doc.getTitle());
                }
            }
        }

        Object[] params = { nbPublishedDocs };
        facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 " +
                resourcesAccessor.getMessages().get("n_published_docs"), params);

        if (nbPublishedDocs < docs2Publish.size()) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "selection_contains_non_publishable_docs"));
        }

        EventManager.raiseEventsOnDocumentChildrenChange(navigationContext.getCurrentDocument());
        return null;
    }

    /*
     * Called locally and also by FileManageActionsBean.moveWithId
     */
    public DocumentModel publishDocument(final DocumentModel docToPublish,
            final DocumentModel section) throws ClientException {

        // Must have READ on target section
        if (!documentManager.hasPermission(section.getRef(),
                SecurityConstants.READ)) {
            throw new ClientException(
                    "Cannot publish because not enough rights");
        }

        // set issued date only if the doc is dirty, to avoid setting it
        // repeatedly if several publishings are done
        final boolean setIssuedDate = documentManager.isDirty(docToPublish.getRef()) &&
                !docToPublish.isProxy();
        if (setIssuedDate) {
            docToPublish.setProperty("dublincore", "issued",
                    Calendar.getInstance());
            // make sure that saveDocument doesn't create a snapshot,
            // as publishDocument will do it
            docToPublish.putContextData(
                    org.nuxeo.common.collections.ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY,
                    Boolean.FALSE);
        }

        DocumentModel proxy;

        /*
         * If not enough rights to creating content, bypass rights since READ is
         * enough for publishing.
         */
        if (!documentManager.hasPermission(section.getRef(),
                SecurityConstants.ADD_CHILDREN)) {
            String repositoryName = docToPublish.getRepositoryName();
            DocumentModel[] misc = new DocumentModel[] { null };
            new UnrestrictedSessionRunner(repositoryName, misc) {
                @Override
                public void run() throws ClientException {

                    if (setIssuedDate) {
                        unrestrictedSession.saveDocument(docToPublish);
                    }
                    DocumentModel proxy = unrestrictedSession.publishDocument(
                            docToPublish, section);
                    unrestrictedSession.save();

                    // Fire an event for the publication workflow.
                    // NuxeoPrincipal principal = (NuxeoPrincipal)
                    // FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
                    // eventManager.raiseEventsOnDocumentSubmitedForPublication(docToPublish,
                    // section, principal, comment);

                    Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
                    eventInfo.put("targetSection", section.getName());
                    String[] validators = getPublishingService().getValidatorsFor(
                            proxy);
                    StringBuilder recips = new StringBuilder();
                    for (String user : validators) {
                        boolean isUser = principalListManager.getPrincipalType(user) == PrincipalListManager.USER_TYPE;
                        recips.append((isUser ? "user:" : "group:") + user +
                                "|");
                    }
                    eventInfo.put("recipients", recips.substring(0,
                            recips.length() - 1));

                    notifyEvent(
                            org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION,
                            eventInfo, comment, null, docToPublish);

                    /*
                     * Invalidate dashboard items using Seam since a publishing
                     * workflow might have been started. XXX We need to do it
                     * here since the workflow starts in a message driven bean
                     * in a async way. Not sure we can optimize right now.
                     */
                    Events.instance().raiseEvent(
                            org.nuxeo.ecm.platform.workflow.api.client.events.EventNames.WORKFLOW_NEW_STARTED);

                    ((DocumentModel[]) misc)[0] = proxy; // pass to caller
                }
            }.runUnrestricted();
            proxy = misc[0]; // get info from inner method

        } else {
            if (setIssuedDate) {
                documentManager.saveDocument(docToPublish);
            }
            proxy = documentManager.publishDocument(docToPublish, section);
            documentManager.save();
            // fire an event for the publication workflow
            // NuxeoPrincipal principal = (NuxeoPrincipal)
            // FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
            // eventManager.raiseEventsOnDirectDocumentPublish(docToPublish,
            // section, principal, comment);
            Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
            eventInfo.put("targetSection", section.getName());
            eventInfo.put("proxy", proxy);
            eventInfo.put("sectionPath", section.getPathAsString());
            notifyEvent(
                    org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLISHED,
                    eventInfo, comment, null, docToPublish);
        }

        return proxy;
    }

    @Destroy
    public void destroy() {
    }

    /*
     * Called by Seam remoting.
     */
    @WebRemote
    public String processRemoteSelectRowEvent(String docRef, Boolean selection)
            throws ClientException {
        log.debug("Selection processed  : " + docRef);
        List<SelectDataModelRow> sections = getSectionsModel().getRows();

        SelectDataModelRow rowToSelect = null;
        DocumentModelTreeNode node = null;

        for (SelectDataModelRow d : sections) {
            if (((DocumentModelTreeNode) d.getData()).getDocument().getRef().toString().equals(
                    docRef)) {
                node = (DocumentModelTreeNode) d.getData();
                rowToSelect = d;
                break;
            }
        }
        if (node == null) {
            return "ERROR : DataNotFound";
        }

        rowToSelect.setSelected(selection);
        if (selection) {
            getSelectedSections().add(node);
        } else {
            getSelectedSections().remove(node);
        }

        return "OK";
    }

    /*
     * Called by document_publish.xhtml
     */
    public SelectDataModel getSectionsModel() throws ClientException {
        if (sectionsModel == null) {
            getSectionsSelectModel();
        }
        return sectionsModel;
    }

    protected void setSectionsModel(SelectDataModel sectionsModel) {
        this.sectionsModel = sectionsModel;
    }

    @Observer(value = EventNames.DOCUMENT_SELECTION_CHANGED, create = false, inject = false)
    public void cancelTheSections() {
        setSectionsModel(null);
        setSelectedSections(null);
        existingPublishedProxy = null;
    }

    /**
     * @deprecated Unused.
     */
    @Deprecated
    public void processSelectRowEvent(SelectDataModelRowEvent event) {
    }

    public List<DocumentModelTreeNode> getSelectedSections() {
        log.debug("getSelectedSections");
        if (selectedSections == null) {
            selectedSections = new ArrayList<DocumentModelTreeNode>();

        }
        return selectedSections;
    }

    private void addSelectedSection(DocumentModelTreeNode section) {
        if (selectedSections == null) {
            selectedSections = new ArrayList<DocumentModelTreeNode>();
        }

        Boolean sectionAlreadySelected = false;
        for (DocumentModelTreeNode node : selectedSections) {
            if (node.getDocument().getRef().equals(
                    section.getDocument().getRef())) {
                sectionAlreadySelected = true;
                break;
            }
        }
        if (!sectionAlreadySelected) {
            selectedSections.add(section);
        }
    }

    public void setSelectedSections(List<DocumentModelTreeNode> selectedSections) {
        log.debug("Set Selected Sections");
        this.selectedSections = selectedSections;
    }

    public void notifyEvent(String eventId,
            Map<String, Serializable> properties, String comment,
            String category, DocumentModel dm) throws ClientException {

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }

        properties.put(CoreEventConstants.REPOSITORY_NAME,
                documentManager.getRepositoryName());
        properties.put(CoreEventConstants.SESSION_ID,
                documentManager.getSessionId());
        properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                dm.getCurrentLifeCycleState());

        CoreEvent event = new CoreEventImpl(eventId, dm, properties,
                documentManager.getPrincipal(), category, comment);

        EventMessage message = new DocumentMessageImpl(dm, event);

        try {
            getDocumentMessageProducer().produce(message);
        } catch (Exception e) {
            throw new ClientException(e);
        }

    }

    // TODO move to protected
    public void unPublishDocument(DocumentModel proxy) throws ClientException {
        documentManager.removeDocument(proxy.getRef());
        documentManager.save();
        try {
            notifyEvent(
                    org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_UNPUBLISHED,
                    null, comment, null, navigationContext.getCurrentDocument());
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    // TODO move to protected
    public void unPublishDocuments(List<DocumentModel> documentsList)
            throws ClientException {
        Object[] params = { documentsList.size() };
        List<DocumentRef> documentsRef = new ArrayList<DocumentRef>();
        for (DocumentModel document : documentsList) {
            documentsRef.add(document.getRef());
            if (document.isProxy() && document.getSourceId() != null) {
                try {
                    String proxySourceId = documentManager.getDocument(
                            new IdRef(document.getSourceId())).getSourceId();
                    notifyEvent(
                            org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_UNPUBLISHED,
                            null,
                            null,
                            null,
                            documentManager.getDocument(new IdRef(proxySourceId)));

                } catch (Exception e) {
                    throw new ClientException(e);
                }
            }

        }

        // remove from the current selection list
        documentsListsManager.resetWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);

        documentManager.removeDocuments(documentsRef.toArray(new DocumentRef[0]));
        documentManager.save();

        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get("n_unpublished_docs"),
                params);
    }

    /*
     * Called by document_publish.xhtml
     */
    public String unPublishDocument() throws ClientException {

        for (DocumentModelTreeNode section : getSelectedSections()) {

            if (section.getDocument().getRef().toString().equals(
                    unPublishSectionRef)) {

                for (DocumentModel doc : getProxies(navigationContext.getCurrentDocument())) {

                    if (doc.getParentRef().toString().equals(
                            unPublishSectionRef)) {
                        unPublishDocument(doc);
                        facesMessages.add(FacesMessage.SEVERITY_INFO,
                                resourcesAccessor.getMessages().get(
                                        "document_unpublished"),
                                resourcesAccessor.getMessages().get(
                                        doc.getType()));

                    }
                }
            }
        }
        setSectionsModel(null);
        setSelectedSections(null);
        return null;
    }

    /*
     * Called by action CURRENT_SELECTION_UNPUBLISH.
     */
    public void unPublishDocumentsFromCurrentSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION)) {
            unPublishDocuments(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION));
        } else {
            log.debug("No selectable Documents in context to process unpublish on...");
        }
        log.debug("Unpublish the selected document(s) ...");
    }

    /*
     * Called by section_clipboard.xhtml
     */
    public List<Action> getActionsForSectionSelection() {
        return webActions.getUnfiltredActionsList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION +
                "_LIST");
    }

    protected PublishingService getPublishingService() throws ClientException {
        if (publishingService == null) {
            // local call first
            publishingService = Framework.getLocalService(PublishingService.class);
            if (publishingService == null) {
                try {
                    publishingService = Framework.getService(PublishingService.class);
                } catch (Exception e) {
                    throw new ClientException(e);
                }
            }
        }
        return publishingService;
    }

    /*
     * Called by publish_buttons.xhtml
     */
    public String getComment() {
        return comment;
    }

    /*
     * Called by publish_buttons.xhtml
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

}
