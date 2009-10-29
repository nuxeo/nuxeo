package org.nuxeo.dam.webapp.contentbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.dam.webapp.PictureActions;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.rest.api.URLPolicyService;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;

@Name("documentActions")
@Scope(ScopeType.CONVERSATION)
public class DocumentActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentActions.class);

    protected static final long BIG_FILE_SIZE_LIMIT = 1024 * 1024 * 5;

    @In(create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient Context conversationContext;

    @In(create = true)
    protected PictureActions pictureActions;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    /**
     * Current selected asset
     */
    protected DocumentModel currentSelection;

    /**
     * Current selection link - defines the fragment to be shown under the tabs
     * list
     */
    protected String currentSelectionLink;

    protected String displayMode = "view";

    protected String downloadSize = "Original:content";

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

    private String handleError(String errorMessage) {
        log.error(errorMessage);
        return "ERROR: " + errorMessage;
    }

    private String computeSelectionActions(String listName) {
        List<Action> availableActions = webActions.getUnfiltredActionsList(listName
                + "_LIST");
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

    public DocumentModel getCurrentSelection() {
        return currentSelection;
    }

    public void setCurrentSelection(DocumentModel selection) {
        // Reset the tabs list and the display mode
        webActions.resetTabList();
        displayMode = "view";

        currentSelection = selection;

        // Set first tab as current tab
        List<Action> tabList = webActions.getTabsList();
        if (tabList != null && tabList.size() > 0) {
            Action currentAction = tabList.get(0);
            webActions.setCurrentTabAction(currentAction);
            currentSelectionLink = currentAction.getLink();
        }

        resetData();
    }

    public String getCurrentSelectionLink() {
        if (currentSelectionLink == null) {
            return "/incl/tabs/empty_tab.xhtml";
        }
        return currentSelectionLink;
    }

    public void setCurrentTabAction(Action currentTabAction) {
        webActions.setCurrentTabAction(currentTabAction);
        currentSelectionLink = currentTabAction.getLink();
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void toggleDisplayMode() {
        if ("view".equals(displayMode)) {
            displayMode = "edit";
        } else {
            displayMode = "view";
        }
    }

    public void updateCurrentSelection() throws ClientException {
        if (currentSelection != null) {
            documentManager.saveDocument(currentSelection);
            documentManager.save();

            // Switch to view mode
            displayMode = "view";
        }
    }

    /**
     * Logs the user out. Invalidates the HTTP session so that it cannot be used
     * anymore.
     *
     * @return the next page that is going to be displayed
     * @throws IOException
     */
    public static String logout() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        Object req = eContext.getRequest();
        Object resp = eContext.getResponse();
        HttpServletRequest request = null;
        if (req instanceof HttpServletRequest) {
            request = (HttpServletRequest) req;
        }
        HttpServletResponse response = null;
        if (resp instanceof HttpServletResponse) {
            response = (HttpServletResponse) resp;
        }

        if (response != null && request != null
                && !context.getResponseComplete()) {
            String baseURL = BaseURL.getBaseURL(request);
            request.setAttribute(URLPolicyService.DISABLE_REDIRECT_REQUEST_KEY,
                    true);
            response.sendRedirect(baseURL + NXAuthConstants.LOGOUT_PAGE);
            context.responseComplete();
        }
        return null;
    }

    /**
     * Takes in a DocumentModel, gets the 'title' from it, and crops it to a
     * maximum of maxLength characters. If the Title is more than maxLength
     * characters it will return the Beginning of the title, followed by 3
     * ellipses (...) followed by the End of the title.
     *
     * A minimum of 6 characters is needed before cropping takes effect. If you
     * specify a maxLength of less than 5, it is ignored - in this case
     * maxLength will be set to begin at 5.
     *
     * @param DocumentModel document to extract the title from
     * @param int maxLength the maximum length of the title before cropping will
     *        occur
     * @return String with the cropped title restricted to maximum of maxLength
     *         characters
     */
    public String getTitleCropped(DocumentModel document, int maxLength) {

        String title = null;
        String beginningChars = null;
        int nbrBeginningChars = -1;
        String endChars = null;
        int nbrEndChars = -1;
        int nbrEllipses = 3;
        int minLength = 5;

        title = DocumentModelFunctions.titleOrId(document);

        int length = title.length();

        // a minimum of 5 characters needed before we crop
        if (length <= minLength) {
            return title;
        }

        // if maxLength is crazy, set it to a proper value
        if (maxLength <= minLength) {
            maxLength = minLength;
        }

        if (length <= maxLength) {
            return title;
        }

        // at this point we should be ok to start cropping to our heart's
        // content
        // length is more than maxLength characters: construct the new title

        // get the first (maxLength-3)/2 characters:
        if ((maxLength - nbrEllipses) % 2 == 0) {
            nbrBeginningChars = (maxLength - nbrEllipses) / 2;
        } else {
            nbrBeginningChars = (maxLength - nbrEllipses) / 2 + 1;
        }

        beginningChars = title.substring(0, nbrBeginningChars);
        // get the last n characters:
        nbrEndChars = maxLength - nbrBeginningChars - nbrEllipses;
        endChars = title.substring(length - nbrEndChars, length);

        String croppedTitle = beginningChars + "..." + endChars;
        return croppedTitle;

    }

    public void download(DocumentView docView) throws ClientException {
        if (docView != null) {
            DocumentLocation docLoc = docView.getDocumentLocation();
            if (documentManager == null) {
                RepositoryLocation loc = new RepositoryLocation(
                        docLoc.getServerName());
                documentManager = getOrCreateDocumentManager(loc);
            }
            DocumentModel doc = documentManager.getDocument(docLoc.getDocRef());
            if (doc != null) {
                // get properties from document view
                Blob blob = DocumentFileCodec.getBlob(doc, docView);
                String filename = DocumentFileCodec.getFilename(doc, docView);
                // download
                FacesContext context = FacesContext.getCurrentInstance();
                if (blob.getLength() > BIG_FILE_SIZE_LIMIT) {
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

    protected CoreSession getOrCreateDocumentManager(
            RepositoryLocation repositoryLocation) throws ClientException {
        if (documentManager != null) {
            return documentManager;
        }
        DocumentManagerBusinessDelegate documentManagerBD = (DocumentManagerBusinessDelegate) Contexts.lookupInStatefulContexts("documentManager");
        if (documentManagerBD == null) {
            // this is the first time we select the location, create a
            // DocumentManagerBusinessDelegate instance
            documentManagerBD = new DocumentManagerBusinessDelegate();
            conversationContext.set("documentManager", documentManagerBD);
        }
        documentManager = documentManagerBD.getDocumentManager(repositoryLocation);
        return documentManager;
    }

    public String downloadBlob() throws ClientException {
        if (currentSelection != null) {
            if (currentSelection.hasSchema("file")) {
                DocumentLocation docLoc = new DocumentLocationImpl(
                        currentSelection);
                Map<String, String> params = new HashMap<String, String>();
                params.put(DocumentFileCodec.FILE_PROPERTY_PATH_KEY,
                        "file:content");
                params.put(
                        DocumentFileCodec.FILENAME_KEY,
                        (String) currentSelection.getPropertyValue("file:filename"));
                DocumentView docView = new DocumentViewImpl(docLoc, null,
                        params);

                download(docView);
            }

            if (currentSelection.hasSchema("picture")) {
                DocumentLocation docLoc = new DocumentLocationImpl(
                        currentSelection);
                Map<String, String> params = new HashMap<String, String>();
                params.put(DocumentFileCodec.FILE_PROPERTY_PATH_KEY,
                        downloadSize);
                DocumentView docView = new DocumentViewImpl(docLoc, null,
                        params);

                pictureActions.downloadPicture(docView);
            }
        }

        return null;
    }

    private void resetData() {
        // Data to reset on asset selection is changed
        downloadSize = "Original:content";
    }

    public String getDownloadSize() {
        return downloadSize;
    }

    public void setDownloadSize(String downloadSize) {
        this.downloadSize = downloadSize;
    }

    public void rewind(PagedDocumentsProvider provider) {
        provider.rewind();
        setCurrentSelectionBasedOnProvider(provider);
    }

    public void previous(PagedDocumentsProvider provider) {
        provider.previous();
        setCurrentSelectionBasedOnProvider(provider);
    }

    public void next(PagedDocumentsProvider provider) {
        provider.next();
        setCurrentSelectionBasedOnProvider(provider);
    }

    public void last(PagedDocumentsProvider provider) {
        provider.last();
        setCurrentSelectionBasedOnProvider(provider);
    }

    private void setCurrentSelectionBasedOnProvider(
            PagedDocumentsProvider provider) {
        // CB: DAM-235 - On a page, first asset must be always selected
        DocumentModelList currentPage = provider.getCurrentPage();
        if (currentPage != null && !currentPage.isEmpty()) {
            currentSelection = currentPage.get(0);
        }
    }
}
