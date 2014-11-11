/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.webapp.contentbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.action.DeleteActions;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.runtime.api.Framework;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 * @author M.-A. Darche
 */
@Name("documentActions")
@Scope(CONVERSATION)
public class DocumentActionsBean extends InputController implements
        DocumentActions, Serializable {

    private static final long serialVersionUID = -2069669959016643607L;

    private static final Log log = LogFactory.getLog(DocumentActionsBean.class);

    public static String DEFAULT_SUMMARY_LAYOUT = "default_summary_layout";

    public static String LIFE_CYCLE_TRANSITION_KEY = "lifeCycleTransition";

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    @RequestParameter
    protected String fileFieldFullName;

    @RequestParameter
    protected String filenameFieldFullName;

    @RequestParameter
    protected String filename;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected transient DeleteActions deleteActions;

    @In(create = true)
    protected transient WebActions webActions;

    protected String comment;

    // @Create
    public void initialize() {
        log.debug("Initializing...");
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
    }

    @Factory(autoCreate = true, value = "currentDocumentSummaryLayout", scope = EVENT)
    public String getCurrentDocumentSummaryLayout() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null) {
            return null;
        }
        String[] layouts = typeManager.getType(doc.getType()).getLayouts(
                BuiltinModes.SUMMARY, null);

        if (layouts != null && layouts.length > 0) {
            return layouts[0];
        }
        return DEFAULT_SUMMARY_LAYOUT;
    }

    @Factory(autoCreate = true, value = "currentDocumentType", scope = EVENT)
    public Type getCurrentType() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null) {
            return null;
        }
        return typeManager.getType(doc.getType());
    }

    public Type getChangeableDocumentType() {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        if (changeableDocument == null) {
            // should we really do this ???
            navigationContext.setChangeableDocument(navigationContext.getCurrentDocument());
            changeableDocument = navigationContext.getChangeableDocument();
        }
        if (changeableDocument == null) {
            return null;
        }
        return typeManager.getType(changeableDocument.getType());
    }

    /**
     * Returns the edit view of a document.
     */
    public String editDocument() throws ClientException {
        navigationContext.setChangeableDocument(navigationContext.getCurrentDocument());
        return navigationContext.navigateToDocument(
                navigationContext.getCurrentDocument(), "edit");
    }

    public String getFileName(DocumentModel doc) throws ClientException {
        try {
            String name = null;
            if (filename != null && !"".equals(filename)) {
                name = filename;
            } else {
                // try to fetch it from given field
                if (filenameFieldFullName != null) {
                    String[] s = filenameFieldFullName.split(":");
                    try {
                        name = (String) doc.getProperty(s[0], s[1]);
                    } catch (ArrayIndexOutOfBoundsException err) {
                        // ignore, filename is not really set
                    }
                }
                // try to fetch it from title
                if (name == null || "".equals(name)) {
                    name = (String) doc.getProperty("dublincore", "title");
                }
            }
            return name;

        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public String download() throws ClientException {
        try {
            if (fileFieldFullName == null) {
                return null;
            }

            String[] s = fileFieldFullName.split(":");
            Blob blob = (Blob) currentDocument.getProperty(s[0], s[1]);
            String filename = getFileName(currentDocument);
            FacesContext context = FacesContext.getCurrentInstance();
            return ComponentUtils.download(context, blob, filename);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    public void download(DocumentView docView) throws ClientException {
        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            // fix for NXP-1799
            if (documentManager == null) {
                RepositoryLocation loc = new RepositoryLocation(
                        docLoc.getServerName());
                navigationContext.setCurrentServerLocation(loc);
                documentManager = navigationContext.getOrCreateDocumentManager();
            }
            DocumentModel doc = documentManager.getDocument(docLoc.getDocRef());
            if (doc != null) {
                // get properties from document view
                Blob blob = DocumentFileCodec.getBlob(doc, docView);
                if (blob == null) {
                    log.warn("No blob for docView: " + docView);
                    return;
                }
                String filename = DocumentFileCodec.getFilename(doc, docView);
                // download
                FacesContext context = FacesContext.getCurrentInstance();
                if (blob.getLength() > Functions.getBigFileSizeLimit()) {
                    HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
                    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

                    String bigDownloadURL = BaseURL.getBaseURL(request);
                    bigDownloadURL += "nxbigfile" + "/";
                    bigDownloadURL += doc.getRepositoryName() + "/";
                    bigDownloadURL += doc.getRef().toString() + "/";
                    bigDownloadURL += docView.getParameter(DocumentFileCodec.FILE_PROPERTY_PATH_KEY)
                            + "/";
                    bigDownloadURL += URIUtils.quoteURIPathComponent(filename,
                            true);
                    try {
                        response.sendRedirect(bigDownloadURL);
                    } catch (IOException e) {
                        log.error(
                                "Error while redirecting for big file downloader",
                                e);
                    }
                } else {
                    ComponentUtils.download(context, blob, filename);
                }
            }
        }
    }

    // XXX AT: broken right now
    public String downloadFromList() throws ClientException {
        try {
            // DocumentModel docMod = getDataTableModel().getSelectedDocModel();
            DocumentModel docMod = null;
            if (docMod == null || fileFieldFullName == null) {
                return null;
            }

            String[] s = fileFieldFullName.split(":");
            Blob blob = (Blob) docMod.getProperty(s[0], s[1]);
            if (blob == null) {
                log.error("No bytes available for the file");
                return null;
            }

            String filename = getFileName(currentDocument);
            if (filename == null || "".equals(filename)) {
                filename = "file";
            }

            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + filename + "\";");
            log.debug("Downloading with mime/type : " + blob.getMimeType());
            response.setContentType(blob.getMimeType());
            // response.setCharacterEncoding(blob.getEncoding());
            response.getOutputStream().write(blob.getByteArray());
            response.flushBuffer();
            context.responseComplete();
            return null;
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    /**
     * Updates document considering that current document model holds edited
     * values.
     * <p>
     * Method called from page action.
     *
     * @deprecated should update changeableDocument and use updateDocument
     */
    @Deprecated
    // TODO: remove (not used)
    public String updateCurrentDocument() throws ClientException {
        try {
            currentDocument = documentManager.saveDocument(currentDocument);
            throwUpdateComments(currentDocument);
            documentManager.save();
            // not navigationContext.saveCurrentDocument();

            facesMessages.add(
                    FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_modified"),
                    resourcesAccessor.getMessages().get(
                            currentDocument.getType()));
            eventManager.raiseEventsOnDocumentChange(currentDocument);
            return navigationContext.navigateToDocument(currentDocument,
                    "after-edit");
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    /**
     * Saves changes hold by the changeableDocument document model.
     */
    public String updateDocument() throws ClientException {
        try {
            DocumentModel changeableDocument = navigationContext.getChangeableDocument();
            Events.instance().raiseEvent(EventNames.BEFORE_DOCUMENT_CHANGED,
                    changeableDocument);
            changeableDocument = documentManager.saveDocument(changeableDocument);
            throwUpdateComments(changeableDocument);
            documentManager.save();
            // some changes (versioning) happened server-side, fetch new one
            navigationContext.invalidateCurrentDocument();
            facesMessages.add(
                    FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_modified"),
                    resourcesAccessor.getMessages().get(
                            changeableDocument.getType()));
            eventManager.raiseEventsOnDocumentChange(changeableDocument);
            return navigationContext.navigateToDocument(changeableDocument,
                    "after-edit");
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    /**
     * Saves changes in current version and then create a new current one.
     */
    public String updateDocumentAsNewVersion() throws ClientException {
        try {
            DocumentModel changeableDocument = navigationContext.getChangeableDocument();
            changeableDocument.putContextData(
                    org.nuxeo.common.collections.ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY,
                    Boolean.TRUE);
            changeableDocument = documentManager.saveDocument(changeableDocument);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("new_version_created"));
            // then follow the standard pageflow for edited documents
            // return result;

            eventManager.raiseEventsOnDocumentChange(changeableDocument);
            return navigationContext.navigateToDocument(changeableDocument,
                    "after-edit");
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    /**
     * Returns the create view of a document type.
     */
    public String createDocument() throws ClientException {
        Type docType = typesTool.getSelectedType();
        return createDocument(docType.getId());
    }

    /**
     * Returns the create view of given document type.
     */
    public String createDocument(String typeName) throws ClientException {
        Type docType = typeManager.getType(typeName);
        // we cannot use typesTool as intermediary since the DataModel callback
        // will alter whatever type we set
        typesTool.setSelectedType(docType);
        try {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put(CoreEventConstants.PARENT_PATH,
                    navigationContext.getCurrentDocument().getPathAsString());
            DocumentModel changeableDocument = documentManager.createDocumentModel(
                    typeName, context);
            navigationContext.setChangeableDocument(changeableDocument);
            return navigationContext.getActionResult(changeableDocument,
                    UserAction.CREATE);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    /**
     * Badly named method that actually creates a document.
     */
    public String saveDocument() throws ClientException {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        return saveDocument(changeableDocument);
    }

    @RequestParameter
    protected String parentDocumentPath;

    public String saveDocument(DocumentModel newDocument)
            throws ClientException {
        // Document has already been created if it has an id.
        // This will avoid creation of many documents if user hit create button
        // too many times.
        if (newDocument.getId() != null) {
            log.debug("Document " + newDocument.getName() + " already created");
            return navigationContext.navigateToDocument(newDocument,
                    "after-create");
        }
        try {
            PathSegmentService pss;
            try {
                pss = Framework.getService(PathSegmentService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            if (parentDocumentPath == null) {
                if (currentDocument == null) {
                    // creating item at the root
                    parentDocumentPath = documentManager.getRootDocument().getPathAsString();
                } else {
                    parentDocumentPath = navigationContext.getCurrentDocument().getPathAsString();
                }
            }

            newDocument.setPathInfo(parentDocumentPath,
                    pss.generatePathSegment(newDocument));

            newDocument = documentManager.createDocument(newDocument);
            documentManager.save();

            logDocumentWithTitle("Created the document: ", newDocument);
            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_saved"),
                    resourcesAccessor.getMessages().get(newDocument.getType()));

            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);
            return navigationContext.navigateToDocument(newDocument,
                    "after-create");
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    // SelectModel to use in interface
    // GR: for now the factory contexts don't play well with
    // resultsProviderCache
    // SelectDataModel building should be cheap anyway
    // @Factory(value = "documentActions_childrenSelectModel", scope = EVENT)

    @Factory(value = "currentChildrenSelectModel", scope = EVENT)
    public SelectDataModel getChildrenSelectModel() throws ClientException {
        // XXX : this proves that this method is called too many times
        // log.debug("Getter children select model");
        DocumentModelList documents = navigationContext.getCurrentDocumentChildrenPage();
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        SelectDataModel model = new SelectDataModelImpl(CHILDREN_DOCUMENT_LIST,
                documents, selectedDocuments);
        model.addSelectModelListener(this);
        // XXX AT: see if cache is useful
        // cacheUpdateNotifier.addCacheListener(model);
        return model;
    }

    // SelectModel to use in interface
    // GR: for now the factory contexts don't play well with
    // resultsProviderCache
    // SelectDataModel building should be cheap anyway
    // @Factory(value = "documentActions_childrenSelectModel", scope = EVENT)
    public SelectDataModel getSectionChildrenSelectModel()
            throws ClientException {
        // XXX : this proves that this method is called too many times
        // log.debug("Getter children select model");
        DocumentModelList documents = navigationContext.getCurrentDocumentChildrenPage();
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);
        SelectDataModel model = new SelectDataModelImpl(CHILDREN_DOCUMENT_LIST,
                documents, selectedDocuments);
        model.addSelectModelListener(this);
        // XXX AT: see if cache is useful
        // cacheUpdateNotifier.addCacheListener(model);
        return model;
    }

    // SelectModelListener interface

    public void processSelectRowEvent(SelectDataModelRowEvent event) {
        // could use source to get to the SelectModel and retrieve its name, but
        // useless here as only one table is involved.
        // SelectModelRow row = event.getRow();
        Boolean selection = event.getSelected();
        DocumentModel data = (DocumentModel) event.getRowData();
        if (selection) {
            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION, data);
        } else {
            documentsListsManager.removeFromWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION, data);
        }
    }

    private String handleError(String errorMessage) {
        log.error(errorMessage);
        return "ERROR: " + errorMessage;
    }

    /**
     * Handle row selection event after having ensured that the navigation
     * context stills points to currentDocumentRef to protect against browsers'
     * back button errors
     *
     * @throws ClientException if currentDocRef is not a valid document
     */
    @WebRemote
    public String checkCurrentDocAndProcessSelectRow(String docRef,
            String providerName, String listName, Boolean selection,
            String currentDocRef) throws ClientException {
        DocumentRef currentDocumentRef = new IdRef(currentDocRef);
        if (!currentDocumentRef.equals(navigationContext.getCurrentDocument().getRef())) {
            navigationContext.navigateToRef(currentDocumentRef);
        }
        return processSelectRow(docRef, providerName, listName, selection);
    }

    @WebRemote
    public String processSelectRow(String docRef, String providerName,
            String listName, Boolean selection) {
        PagedDocumentsProvider provider;
        try {
            provider = resultsProvidersCache.get(providerName);
        } catch (ClientException e) {
            return handleError(e.getMessage());
        }
        DocumentModel doc = null;
        for (DocumentModel pagedDoc : provider.getCurrentPage()) {
            if (pagedDoc.getRef().toString().equals(docRef)) {
                doc = pagedDoc;
                break;
            }
        }
        if (doc == null) {
            return handleError(String.format(
                    "could not find doc '%s' in the current page of provider '%s'",
                    docRef, providerName));
        }
        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;
        if (selection) {
            documentsListsManager.addToWorkingList(lName, doc);
        } else {
            documentsListsManager.removeFromWorkingList(lName, doc);
        }
        return computeSelectionActions(lName);
    }

    /**
     * Handle complete table selection event after having ensured that the
     * navigation context stills points to currentDocumentRef to protect against
     * browsers' back button errors
     *
     * @throws ClientException if currentDocRef is not a valid document
     */
    @WebRemote
    public String checkCurrentDocAndProcessSelectPage(String providerName,
            String listName, Boolean selection, String currentDocRef)
            throws ClientException {
        DocumentRef currentDocumentRef = new IdRef(currentDocRef);
        if (!currentDocumentRef.equals(navigationContext.getCurrentDocument().getRef())) {
            navigationContext.navigateToRef(currentDocumentRef);
        }
        return processSelectPage(providerName, listName, selection);
    }

    @WebRemote
    public String processSelectPage(String providerName, String listName,
            Boolean selection) {
        PagedDocumentsProvider provider;
        try {
            provider = resultsProvidersCache.get(providerName);
        } catch (ClientException e) {
            return handleError(e.getMessage());
        }
        DocumentModelList documents = provider.getCurrentPage();
        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;
        if (selection) {
            documentsListsManager.addToWorkingList(lName, documents);
        } else {
            documentsListsManager.removeFromWorkingList(lName, documents);
        }
        return computeSelectionActions(lName);
    }

    private String computeSelectionActions(String listName) {
        List<Action> availableActions = webActions.getUnfiltredActionsList(listName
                + "_LIST");

        // add Orderable actions
        availableActions.addAll(webActions.getUnfiltredActionsList("ORDERABLE_"
                + listName + "_LIST"));

        List<String> availableActionIds = new ArrayList<String>();
        for (Action a : availableActions) {
            if (a.getAvailable()) {
                availableActionIds.add(a.getId());
            }
        }
        String res = "";
        if (!availableActionIds.isEmpty()) {
            res = StringUtils.join(availableActionIds.toArray(), "|");
        }
        return res;
    }

    public boolean getWriteRight() throws ClientException {
        // TODO: WRITE is a high level compound permission (i.e. more like a
        // user
        // profile), public methods of the Nuxeo framework should only check
        // atomic / specific permissions such as WRITE_PROPERTIES, REMOVE,
        // ADD_CHILDREN depending on the action to execute instead
        return documentManager.hasPermission(
                navigationContext.getCurrentDocument().getRef(),
                SecurityConstants.WRITE);
    }

    // Send the comment of the update to the Core
    private void throwUpdateComments(DocumentModel changeableDocument) {
        if (comment != null && !"".equals(comment)) {
            changeableDocument.getContextData().put("comment", comment);
        }
    }

    public String getComment() {
        return "";
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean getCanUnpublish() {
        List<DocumentModel> docList = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);

        if (!(docList == null || docList.isEmpty())
                && deleteActions.checkDeletePermOnParents(docList)) {
            for (DocumentModel document : docList) {
                if (document.getType().equals("SectionRoot")
                        || document.getType().equals("Section")) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Observer(EventNames.BEFORE_DOCUMENT_CHANGED)
    public void followTransition(DocumentModel changedDocument)
            throws ClientException {
        String transitionToFollow = (String) changedDocument.getContextData(
                ScopeType.REQUEST, LIFE_CYCLE_TRANSITION_KEY);
        if (transitionToFollow != null) {
            documentManager.followTransition(changedDocument.getRef(),
                    transitionToFollow);
            documentManager.save();
        }
    }

}
