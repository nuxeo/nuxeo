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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.dam.platform.action.DamWebActions.DAM_VIEW_ASSET_ACTION_LIST_CATEGORY;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.nuxeo.dam.webapp.chainselect.ChainSelectCleaner;
import org.nuxeo.dam.webapp.helper.DownloadHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.web.listener.ejb.ContentHistoryActionsBean;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;
import org.nuxeo.ecm.webapp.helpers.EventNames;

@Name("damDocumentActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DamDocumentActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(DamDocumentActions.class);

    protected static final String DEFAULT_PICTURE_DOWNLOAD_PROPERTY = "Original";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient Context conversationContext;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    private transient NuxeoPrincipal currentNuxeoPrincipal;

    @In(create = true)
    ContentHistoryActionsBean contentHistoryActions;

    /**
     * Current selected asset
     */
    protected DocumentModel currentSelection;

    /**
     * Current selection link - defines the fragment to be shown under the tabs
     * list
     */
    protected String currentSelectionLink;

    protected boolean showExifArea;

    protected boolean showIptcArea;

    protected String displayMode = BuiltinModes.VIEW;

    protected String downloadSize = DEFAULT_PICTURE_DOWNLOAD_PROPERTY;

    protected List<LogEntry> selectedAssetLogEntries;

    public DocumentModel getCurrentSelection() {
        return currentSelection;
    }

    public void setCurrentSelection(DocumentModel selection) {
        // Reset the tabs list and the display mode
        webActions.resetCurrentTabs(DAM_VIEW_ASSET_ACTION_LIST_CATEGORY);
        displayMode = BuiltinModes.VIEW;
        currentSelection = selection;
        selectedAssetLogEntries = null;
        currentSelectionLink = webActions.getCurrentTabAction(
                DAM_VIEW_ASSET_ACTION_LIST_CATEGORY).getLink();
        resetData();
        raiseEvents(currentSelection);
    }

    public String getCurrentSelectionLink() {
        if (currentSelectionLink == null) {
            return "/incl/tabs/empty_tab.xhtml";
        }
        return currentSelectionLink;
    }

    public void setCurrentTabAction(Action currentTabAction) {
        webActions.setCurrentTabAction(DAM_VIEW_ASSET_ACTION_LIST_CATEGORY,
                currentTabAction);
        currentSelectionLink = currentTabAction.getLink();
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void toggleDisplayMode() {
        if (BuiltinModes.VIEW.equals(displayMode)) {
            displayMode = BuiltinModes.EDIT;
        } else {
            displayMode = BuiltinModes.VIEW;
        }
    }

    public void updateCurrentSelection() throws ClientException {
        if (currentSelection != null) {
            documentManager.saveDocument(currentSelection);
            documentManager.save();

            // Switch to view mode
            displayMode = BuiltinModes.VIEW;
        }
    }

    /**
     * Takes in a DocumentModel, gets the 'title' from it, and crops it to a
     * maximum of maxLength characters. If the Title is more than maxLength
     * characters it will return the Beginning of the title, followed by 3
     * ellipses (...) followed by the End of the title.
     * <p>
     * A minimum of 6 characters is needed before cropping takes effect. If you
     * specify a maxLength of less than 5, it is ignored - in this case
     * maxLength will be set to begin at 5.
     *
     * @param document to extract the title from
     * @param maxLength the maximum length of the title before cropping will
     *            occur
     * @return String with the cropped title restricted to maximum of maxLength
     *         characters
     */
    public String getTitleCropped(DocumentModel document, int maxLength) {
        int nbrEllipses = 3;
        int minLength = 5;

        String title;
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
        int nbrBeginningChars;
        if ((maxLength - nbrEllipses) % 2 == 0) {
            nbrBeginningChars = (maxLength - nbrEllipses) / 2;
        } else {
            nbrBeginningChars = (maxLength - nbrEllipses) / 2 + 1;
        }

        String beginningChars = title.substring(0, nbrBeginningChars);
        // get the last n characters:
        int nbrEndChars = maxLength - nbrBeginningChars - nbrEllipses;
        String endChars = title.substring(length - nbrEndChars, length);

        return beginningChars + "..." + endChars;
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
                String filename = DocumentFileCodec.getFilename(doc, docView);
                // download
                FacesContext context = FacesContext.getCurrentInstance();
                DownloadHelper.download(
                        context,
                        doc,
                        docView.getParameter(DocumentFileCodec.FILE_PROPERTY_PATH_KEY),
                        filename);
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

                Blob blob = currentSelection.getAdapter(BlobHolder.class).getBlob();
                params.put(DocumentFileCodec.FILENAME_KEY, blob.getFilename());
                DocumentView docView = new DocumentViewImpl(docLoc, null,
                        params);

                download(docView);
            } else if (currentSelection.hasSchema("picture")) {
                PictureResourceAdapter pra = currentSelection.getAdapter(PictureResourceAdapter.class);
                String xpath = pra.getViewXPath(downloadSize);
                String filename = (String) currentSelection.getPropertyValue(xpath
                        + "filename");
                String blobXpath = xpath + "content";
                FacesContext context = FacesContext.getCurrentInstance();
                DownloadHelper.download(context, currentSelection, blobXpath,
                        filename);
            }
        }

        return null;
    }

    private void resetData() {
        // Data to reset on asset selection is changed
        downloadSize = DEFAULT_PICTURE_DOWNLOAD_PROPERTY;

        ChainSelectCleaner.cleanup(ChainSelectCleaner.ASSET_COVERAGE_CHAIN_SELECT_ID);
        ChainSelectCleaner.cleanup(ChainSelectCleaner.ASSET_SUBJECTS_CHAIN_SELECT_ID);
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
            selectedAssetLogEntries = null;
        }
    }

    public static void raiseEvents(DocumentModel document) {
        Events eventManager = Events.instance();
        eventManager.raiseEvent(EventNames.DOCUMENT_SELECTION_CHANGED, document);
    }

    public String getFullUserName() {
        String fullName;
        if (currentNuxeoPrincipal != null) {
            fullName = Functions.principalFullName(currentNuxeoPrincipal);
        } else {
            fullName = Functions.principalFullName((NuxeoPrincipal) documentManager.getPrincipal());
        }

        return fullName;
    }

    public boolean canEditAsset() throws ClientException {
        return documentManager.hasPermission(currentSelection.getRef(),
                SecurityConstants.WRITE);
    }

    public boolean isShowExifArea() {
        return showExifArea;
    }

    public void setShowExifArea(boolean showExifArea) {
        this.showExifArea = showExifArea;
    }

    public void toggleExifArea(ActionEvent event) {
        showExifArea = !showExifArea;
    }

    public boolean isShowIptcArea() {
        return showIptcArea;
    }

    public void setShowIptcArea(boolean showIptcArea) {
        this.showIptcArea = showIptcArea;
    }

    public void toggleIptcArea(ActionEvent event) {
        showIptcArea = !showIptcArea;
    }

    public List<LogEntry> getSelectedAssetLogEntries()
            throws AuditException {
        if (selectedAssetLogEntries == null) {
            selectedAssetLogEntries = contentHistoryActions.computeLogEntries(getCurrentSelection());
        }
        return selectedAssetLogEntries;
    }

}
