/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: PublishActionsBean.java 28957 2008-01-11 13:36:52Z tdelprat $
 */

package org.nuxeo.ecm.platform.publishing;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.security.auth.login.LoginContext;

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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModelTree;
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.InvalidProxyDocOperation;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeImpl;
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
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.platform.workflow.api.client.events.EventNames;
import org.nuxeo.ecm.webapp.action.DeleteActions;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.ecm.webapp.security.PrincipalListManager;
import org.nuxeo.ecm.webapp.versioning.DocumentVersioning;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Paslaru Narcis</a>
 *
 */

@Name("publishActions")
@Scope(ScopeType.CONVERSATION)
@Transactional
public class PublishActionsBean extends InputController implements
        PublishActions, Serializable {

    private static final long serialVersionUID = 87657657657565L;

    private static final Log log = LogFactory.getLog(PublishActionsBean.class);

    private static final String PUBLISH_DOCUMENT = "PUBLISH_DOCUMENT";

    private static final String PUBLISH_OUTCOME = "after_publish";

    protected static final String DOMAIN_SECTIONS = "DOMAIN_SECTIONS";

    @In(create = true)
    protected WebActions webActions;

    @In(create = true)
    protected DocumentVersioning documentVersioning;

    @In(create = true)
    transient CoreSession documentManager;

    @In(create = true)
    NuxeoPrincipal currentUser;

    @In(create = true)
    protected DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected DeleteActions deleteActions;

    @In(create = true)
    protected PrincipalListManager principalListManager;

    @In
    protected org.nuxeo.ecm.webapp.security.UserSession userSession;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    // hierarchy of existing sections
    private transient SelectDataModel sectionsModel;

    // list of selected sections
    private List<DocumentModelTreeNode> selectedSections;

    // map to store for each sections the existing published proxies
    // XXX AT: see if there is a better way to retrieve/store this information
    // (see #getPublicationInformation)
    private Map<String, DocumentModel> existingPublishedProxy;

    private transient DocumentMessageProducer docMsgProducer;

    private PublishingService publishingService;

    private String comment;

    /*
     * Rux NXP-1879: Multiple types can be suitable for publishing. So use array
     * instead single element. Better naming.
     */
    private static Set<String> sectionRootTypes;

    private static Set<String> sectionTypes;

    @RequestParameter
    private String unPublishSectionRef;

    /*
     * Rux NXP-1879: Multiple types can be suitable for publishing. So use array
     * instead single element. Also includes the "not null return" in the atomic
     * method. Redundant code eliminated.
     */
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

    public List<Action> getActionsForPublishDocument() {
        return webActions.getActionsList(PUBLISH_DOCUMENT);
    }

    /*
     * Rux NXP-1879: Multiple types can be suitable for publishing. So use array
     * instead single element.
     */
    private Set<String> getTypeNamesForFacet(String facetName) {
        TypeService schemaService;
        try {
            schemaService = (TypeService) Framework.getRuntime().getComponent(
                    TypeService.NAME);
        } catch (Exception e) {
            log.error("Exception in retrieving publish spaces : ", e);
            // Rux NXP-1879: Don't initialize the members here, will occur in
            // the calling method.
            return null;
        }

        Set<String> publishRoots = schemaService.getTypeManager().getDocumentTypeNamesForFacet(
                facetName);
        if (publishRoots == null || publishRoots.isEmpty()) {
            return null;
        }
        return publishRoots;
    }

    public void getSectionsSelectModel() throws ClientException {
        log.debug("Try to get the sections model");

        CoreSession session = null;
        LoginContext context = null;
        Repository repository = null;
        try {
            context = Framework.login();
            RepositoryManager repositoryMgr = Framework.getService(RepositoryManager.class);
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            repository = repositoryMgr.getRepository(currentDocument.getRepositoryName());
            session = repository.open();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        DocumentModel root = session.getRootDocument();

        DocumentModelTree sections = new DocumentModelTreeImpl();
        // Rux NXP-1879
        for (DocumentModel domain : session.getChildren(root.getRef(), "Domain")) {
            // for every domain
            for (String sectionRootNameType : getSectionRootTypes()) {
                // for every 'as section root' type
                for (DocumentModel sectionRoot : session.getChildren(
                        domain.getRef(), sectionRootNameType)) {
                    // for every 'section root'
                    for (String sectionNameType : getSectionTypes()) {
                        // for every 'as section type': accumulate the available
                        // sections
                        accumulateAvailableSections(sections,
                                sectionRoot.getPathAsString(), sectionNameType);
                    }
                }
            }
        }

        // check for already selected sections
        existingPublishedProxy = new HashMap<String, DocumentModel>();
        DocumentModelList publishedProxies = documentManager.getProxies(
                navigationContext.getCurrentDocument().getRef(), null);
        // iterate on existing proxies of the current document
        for (DocumentModel pProxy : publishedProxies) {
            // iterate on section tree
            for (DocumentModelTreeNode node : sections) {
                if (node.getDocument().getRef().equals(pProxy.getParentRef())) {
                    String versionLabel = documentVersioning.getVersionLabel(pProxy);
                    node.setVersion(versionLabel);
                    existingPublishedProxy.put(
                            pProxy.getParentRef().toString(), pProxy);
                    addSelectedSection(node);
                    break;
                }
            }
        }

        SelectDataModel model = new SelectDataModelImpl(SECTIONS_DOCUMENT_TREE,
                sections, getSelectedSections());
        model.addSelectModelListener(this);
        // XXX AT: see if cache is useful
        // cacheUpdateNotifier.addCacheListener(model);
        setSectionsModel(model);

        // NXP-1659: close the system session and logout
        try {
            if (repository != null && session != null) {
                repository.close(session);
            }
            if (context != null) {
                context.logout();
            }
        } catch (Exception e) {
            throw new ClientException(e);
        }

    }

    /*
     * Rux NXP-1879: helper method. The code was moved from above here as it
     * was.
     */
    private void accumulateAvailableSections(DocumentModelTree sections,
            String sectionRootPath, String sectionNameType)
            throws ClientException {

        Object[] params = new Object[]{ sectionRootPath, sectionNameType };

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

        for (DocumentModel currentSection : mainSections) {
            if (documentManager.hasPermission(currentSection.getRef(),
                    SecurityConstants.READ)) {
                int currentLevel = currentSection.getPathAsString().split("/").length;
                DocumentModelTreeNode node = new DocumentModelTreeNodeImpl(
                        currentSection, currentLevel - firstLevel);
                sections.add(node);
            }
        }
    }

    public DocumentModelList getProxies(DocumentModel docModel)
            throws ClientException {
        if (docModel == null) {
            return null;
        } else {
            return documentManager.getProxies(docModel.getRef(), null);
        }
    }

    public List<PublishingInformation> getPublishingInformation(
            DocumentModel docModel) throws ClientException {
        DocumentModelList proxies = getProxies(docModel);
        if (proxies == null) {
            return null;
        } else {
            List<PublishingInformation> info = new ArrayList<PublishingInformation>();
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
    }

    /**
     * Publishes the document to the selected sections.
     */
    public String publishDocument() throws ClientException {
        log.debug("publishDocument()");
        DocumentModel docToPublish = navigationContext.getCurrentDocument();

        if (documentManager.getLock(docToPublish.getRef()) != null) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "error.document.locked.for.publish"));
            return null;

        } else {
            List<DocumentModelTreeNode> selectedSections = getSelectedSections();
            if (selectedSections.isEmpty()) {
                facesMessages.add(FacesMessage.SEVERITY_WARN,
                        resourcesAccessor.getMessages().get(
                                "publish.no_section_selected"));

                return null;
            }

            // Proxy for which we need a moderation. Let's request the
            // moderation after the document manager session has been saved
            // to avoid conflicts in between sync txn and async txn that
            // can start before the end of the sync txn.
            List<DocumentModel> forModeration = new ArrayList<DocumentModel>();

            if (selectedSections != null) {
                log.debug("selected " + selectedSections.size() + " sections");

                for (DocumentModelTreeNode section : selectedSections) {
                    boolean moderation = !isAlreadyPublishedInSection(
                            docToPublish, section.getDocument());

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
                            resourcesAccessor.getMessages().get(
                                    "document_published"),
                            resourcesAccessor.getMessages().get(
                                    docToPublish.getType()));
                }

                if (!forModeration.isEmpty()) {
                    // notifyEvent(org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION,
                    // null, comment,
                    // null, docToPublish);
                    comment = null;
                    facesMessages.add(FacesMessage.SEVERITY_INFO,
                            resourcesAccessor.getMessages().get(
                                    "document_submitted_for_publication"),
                            resourcesAccessor.getMessages().get(
                                    docToPublish.getType()));
                }
            }
            setSectionsModel(null);

            for (DocumentModel proxy : forModeration) {
                notifyEvent(
                        org.nuxeo.ecm.webapp.helpers.EventNames.PROXY_PUSLISHING_PENDING,
                        null, comment, null, proxy);
            }

            return computeOutcome(PUBLISH_OUTCOME);
        }
    }

    public boolean isReviewer(DocumentModel dm) throws ClientException {
        /*
         * Rux NXP-1822: instead of searching for all users having right and
         * looking if the user is contained, just check the right!
         */
        return documentManager.hasPermission(dm.getRef(),
                SecurityConstants.WRITE);
    }

    public boolean isAlreadyPublishedInSection(DocumentModel doc,
            DocumentModel section) throws ClientException {
        boolean published = false;
        // XXX We need refetch here for the new one but we should add this to
        // the seam cache somewhere.
        for (PublishingInformation each : getPublishingInformation(doc)) {
            // FIXME do a better comparison using ref directly. Need to test a
            // bit more since it appears to fail.
            // Furthermore, current DocumentModel implementation performs remote
            // call to get the Path.
            if (each.getSection().getPathAsString().equals(
                    section.getPathAsString())) {
                published = true;
                break;
            }
        }
        return published;
    }

    public String publishWorkList() throws ClientException {
        return publishDocumentList(DocumentsListsManager.DEFAULT_WORKING_LIST);
    }

    public String publishDocumentList(String listName) throws ClientException {

        List<DocumentModel> docs2Publish = documentsListsManager.getWorkingList(listName);
        DocumentModel target = navigationContext.getCurrentDocument();

        // Rux NXP-1879: easier now to check the type
        if (!getSectionTypes().contains(target.getType())) {
            return null;
        }

        int nbPublishedDocs = 0;
        for (DocumentModel doc : docs2Publish) {
            if (!documentManager.hasPermission(doc.getParentRef(),
                    SecurityConstants.WRITE)) {
                continue;
            }

            if (doc.isProxy()) {
                documentManager.copy(doc.getRef(), target.getRef(),
                        doc.getName());
                nbPublishedDocs++;
            } else {
                if (doc.hasFacet(FacetNames.PUBLISHABLE)) {
                    // FIXME JA : I don't know where this is used but it should
                    // use the publishDocument() API of this bean.
                    // fix for EURPCF-338: Publish a Publish document in another
                    // referential generate an error
                    try {
                        documentManager.publishDocument(doc,
                                navigationContext.getCurrentDocument());
                        nbPublishedDocs++;
                    } catch (InvalidProxyDocOperation e) {
                        log.warn("proxy document in list not published: "
                                + doc.getTitle());
                    } catch (ClientException e) {
                        // TODO maybe just inform user about the specific doc
                        // that couldn't be published
                        throw e;
                    }
                }
            }
        }

        Object[] params = { nbPublishedDocs };
        facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                + resourcesAccessor.getMessages().get("n_published_docs"),
                params);

        if (nbPublishedDocs < docs2Publish.size()) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "selection_contains_non_publishable_docs"));
        }

        EventManager.raiseEventsOnDocumentChildrenChange(navigationContext.getCurrentDocument());
        return computeOutcome(PUBLISH_OUTCOME);
    }

    public DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section) throws ClientException {

        // If not read on target section then no way boy
        if (!documentManager.hasPermission(section.getRef(),
                SecurityConstants.READ)) {
            throw new ClientException(
                    "Cannot publish because not enough rights...!");
        }

        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL,
                locale);
        docToPublish.setProperty("dublincore", "issued",
                dateFormat.getCalendar());

        // If not enough rights for creating content over there bypass since
        // READ is enough for publishing.
        DocumentModel proxy = null;
        if (!documentManager.hasPermission(section.getRef(),
                SecurityConstants.ADD_CHILDREN)) {
            try {
                // Login as system to bypass caller principal rights.
                LoginContext loginCtx = Framework.login();
                loginCtx.login();

                // Open a new repository session which will be unrestricted.
                // We need to do this here since the document manager in Seam
                // context has been initialized with caller principal rights.
                RepositoryManager mgr = Framework.getService(RepositoryManager.class);
                CoreSession unrestrictedSession = mgr.getRepository(
                        docToPublish.getRepositoryName()).open();

                unrestrictedSession.saveDocument(docToPublish);

                // Publish the document using the new session.
                proxy = unrestrictedSession.publishDocument(docToPublish,
                        section);

                unrestrictedSession.save();

                // fire an event for the publication workflow
                // NuxeoPrincipal principal = (NuxeoPrincipal)
                // FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
                // eventManager.raiseEventsOnDocumentSubmitedForPublication(docToPublish,
                // section,
                // principal, comment);
                Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
                eventInfo.put("targetSection", section.getName());

                // get validators for section
                String[] validators = getPublishingService().getValidatorsFor(
                        proxy);
                StringBuilder recips = new StringBuilder();
                for (String user : validators) {
                    boolean isUser = principalListManager.getPrincipalType(user) == PrincipalListManager.USER_TYPE;
                    recips.append((isUser ? "user:" : "group:") + user + "|");
                }
                eventInfo.put("recipients", recips.substring(0,
                        recips.length() - 1));

                notifyEvent(
                        org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION,
                        eventInfo, comment, null, docToPublish);

                // To invalidate dashboard items since a publishing workflow
                // might have
                // been started.
                // XXX We need to do it here since the workflow starts in a
                // message
                // driven bean in a async way. Not sure we can optimize right
                // now.
                Events.instance().raiseEvent(
                        org.nuxeo.ecm.platform.workflow.api.client.events.EventNames.WORKFLOW_NEW_STARTED);

                // Close the unrestricted session.
                CoreInstance.getInstance().close(unrestrictedSession);

                // Logout the system session.
                // Note, this is not necessary to take further actions here
                // regarding the user session.
                loginCtx.logout();
            } catch (Exception e) {
                throw new ClientException(e.getMessage());
            }
        } else {
            documentManager.saveDocument(docToPublish);
            proxy = documentManager.publishDocument(docToPublish, section);
            documentManager.save();
            // fire an event for the publication workflow
            // NuxeoPrincipal principal = (NuxeoPrincipal)
            // FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
            // eventManager.raiseEventsOnDirectDocumentPublish(docToPublish,
            // section,
            // principal, comment);
            Map<String, Serializable> eventInfo = new HashMap<String, Serializable>();
            eventInfo.put("targetSection", section.getName());
            notifyEvent(
                    org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLISHED,
                    eventInfo, comment, null, docToPublish);
        }

        return proxy;
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Removing SEAM component: publishActions");
    }

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

        if (selection) {
            getSelectedSections().add(node);
            rowToSelect.setSelected(true);
            log.debug("Section added.");
        } else {
            getSelectedSections().remove(node);
            rowToSelect.setSelected(false);
            log.debug("Section removed.");
        }
        log.debug("Size of sections : " + getSelectedSections().size());

        return "OK";
    }

    /**
     * @return the sectionsModel.
     * @throws ClientException
     */
    public SelectDataModel getSectionsModel() throws ClientException {
        log.debug("Getter called");
        if (sectionsModel == null) {
            getSectionsSelectModel();
        }
        return sectionsModel;
    }

    /**
     * @param sectionsModel the sectionsModel to set.
     */
    public void setSectionsModel(SelectDataModel sectionsModel) {
        log.debug("Set Sections Model " + sectionsModel);
        this.sectionsModel = sectionsModel;
    }

    @Observer(value = EventNames.DOCUMENT_SELECTION_CHANGED, create = false, inject = false)
    public void cancelTheSections() {
        log.debug("Document selection changed");
        setSectionsModel(null);
        setSelectedSections(null);
        existingPublishedProxy = null;
    }

    public void processSelectRowEvent(SelectDataModelRowEvent event)
            throws ClientException {
        log.debug("Select data row. Not used anymore !!!");
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

        CoreSession currentSession = getCurrentSession();
        String repositoryName = currentSession.getRepositoryName();

        // Default category
        if (category == null) {
            category = DocumentEventCategories.EVENT_DOCUMENT_CATEGORY;
        }

        if (properties == null) {
            properties = new HashMap<String, Serializable>();
        }

        // Name of the current repository
        properties.put(CoreEventConstants.REPOSITORY_NAME, repositoryName);

        // Add the session ID
        properties.put(CoreEventConstants.SESSION_ID,
                currentSession.getSessionId());

        properties.put(CoreEventConstants.DOC_LIFE_CYCLE,
                dm.getCurrentLifeCycleState());

        CoreEvent event = new CoreEventImpl(eventId, dm, properties,
                currentSession.getPrincipal(), category, comment);

        EventMessage message = new DocumentMessageImpl(dm, event);

        try {
            getDocumentMessageProducer().produce(message);
        } catch (Exception e) {
            throw new ClientException(e);
        }

    }

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

    public void unPublishDocuments(List<DocumentModel> documentsList)
            throws ClientException {
        Object[] params = { documentsList.size() };
        List<DocumentRef> documentsRef = new ArrayList<DocumentRef>();
        for (DocumentModel document : documentsList) {
            documentsRef.add(document.getRef());
            if (document.isProxy() && document.getSourceId()!=null) {
                try {
                    String proxySourceId = getCurrentSession().getDocument(
                            new IdRef(document.getSourceId())).getSourceId();
                    notifyEvent(
                            org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_UNPUBLISHED,
                            null, null, null, getCurrentSession().getDocument(
                                    new IdRef(proxySourceId)));

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
        log.debug("Unpublish the selected document ...");
        return computeOutcome(PUBLISH_OUTCOME);
    }

    public void unPublishDocumentsFromCurrentSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION)) {
            unPublishDocuments(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION));
        } else {
            log.debug("No selectable Documents in context to process unpublish on...");
        }
        log.debug("Unpublish the selected document(s) ...");
    }

    private CoreSession getCurrentSession() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        CoreSession currentSession = CoreInstance.getInstance().getSession(
                currentDocument.getSessionId());
        if (currentSession == null) {
            String repositoryName = currentDocument.getRepositoryName();
            if (repositoryName == null) {
                log.debug(String.format(
                        "document '%s' has null repositoryName ",
                        currentDocument.getTitle()));
                return null;
            }
            try {
                RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
                currentSession = repositoryManager.getRepository(repositoryName).open();
                if (currentSession == null) {
                    log.debug(String.format("document '%s' has null session ",
                            currentDocument.getTitle()));
                    return null;
                }
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return currentSession;
    }

    public List<Action> getActionsForSectionSelection() {
        return webActions.getUnfiltredActionsList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION
                + "_LIST");
    }

    private PublishingService getPublishingService() throws Exception {
        if (publishingService == null) {
            // local call first
            publishingService = Framework.getLocalService(PublishingService.class);
            if (publishingService == null) {
                publishingService = Framework.getService(PublishingService.class);
            }
        }
        return publishingService;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
