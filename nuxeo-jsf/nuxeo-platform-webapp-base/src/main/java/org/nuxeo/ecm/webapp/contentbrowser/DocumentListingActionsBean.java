/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.contentview.jsf.ContentView;
import org.nuxeo.ecm.platform.contentview.seam.ContentViewActions;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageSelection;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.cache.LRUCachingMap;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Manages document listings rendering.
 *
 * @author Anahide Tchertchian
 */
@Name("documentListingActions")
@Scope(CONVERSATION)
public class DocumentListingActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentListingActionsBean.class);

    public static String DEFAULT_LISTING_LAYOUT = "document_listing";

    @In(create = true)
    protected transient NavigationContext navigationContext;

    /**
     * @since 5.6
     */
    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient ContentViewActions contentViewActions;

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    // only store 20 entries that cache chosen layout for a given document
    protected LRUCachingMap<String, String> docTolistings = new LRUCachingMap<String, String>(20);

    protected String currentListingLayoutName = null;

    protected List<String> currentAvailableListingLayoutNames = null;

    // API for current layout in listing mode

    /**
     * @deprecated this information is now held by content views
     */
    @Deprecated
    public String getLayoutForDocument(DocumentModel doc) {
        if (doc != null) {
            String id = doc.getId();
            if (docTolistings.containsKey(id)) {
                return docTolistings.get(id);
            }
            List<String> availableLayouts = getAvailableLayoutsForDocument(doc);
            if (availableLayouts != null && !availableLayouts.isEmpty()) {
                return availableLayouts.get(0);
            }
        }
        return DEFAULT_LISTING_LAYOUT;
    }

    /**
     * @deprecated this information is now held by content views
     */
    @Deprecated
    public void setLayoutForDocument(DocumentModel doc, String layoutName) {
        if (doc == null) {
            log.error("Cannot set listing layout for null document");
            return;
        }
        String id = doc.getId();
        docTolistings.put(id, layoutName);
    }

    /**
     * @deprecated this information is now held by content views
     */
    @Deprecated
    @Factory(value = "currentListingLayoutName", scope = EVENT)
    public String getLayoutForCurrentDocument() {
        if (currentListingLayoutName == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            currentListingLayoutName = getLayoutForDocument(currentDocument);
        }
        return currentListingLayoutName;
    }

    /**
     * @deprecated this information is now held by content views
     */
    @Deprecated
    public void setLayoutForCurrentDocument(String layoutName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        setLayoutForDocument(currentDocument, layoutName);
        currentListingLayoutName = layoutName;
    }

    /**
     * @deprecated this information is now held by content views
     */
    @Deprecated
    public List<String> getAvailableLayoutsForDocument(DocumentModel doc) {
        if (doc == null) {
            return Collections.emptyList();
        }
        TypeInfo typeInfo = doc.getAdapter(TypeInfo.class);
        String[] layoutNames = typeInfo.getLayouts(BuiltinModes.LISTING, null);
        List<String> res = new ArrayList<String>();
        if (layoutNames != null && layoutNames.length > 0) {
            res.addAll(Arrays.asList(layoutNames));
        } else {
            res.add(DEFAULT_LISTING_LAYOUT);
        }
        return res;
    }

    /**
     * @deprecated this information is now held by content views
     */
    @Deprecated
    @Factory(value = "currentAvailableListingLayoutNames", scope = EVENT)
    public List<String> getAvailableLayoutsForCurrentDocument() {
        if (currentAvailableListingLayoutNames == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            currentAvailableListingLayoutNames = getAvailableLayoutsForDocument(currentDocument);
        }
        return currentAvailableListingLayoutNames;
    }

    /**
     * @deprecated this information is now held by content views
     */
    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    @Deprecated
    public void documentChanged() {
        currentListingLayoutName = null;
        currentAvailableListingLayoutNames = null;
    }

    // API for AJAX selection in listings of content views

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> getCurrentPageDocuments(String contentViewName) {
        List<DocumentModel> documents = null;
        ContentView cView = contentViewActions.getContentView(contentViewName);
        if (cView != null) {
            PageProvider<?> cProvider = cView.getCurrentPageProvider();
            if (cProvider != null) {
                List<?> items = cProvider.getCurrentPage();
                try {
                    documents = (List<DocumentModel>) items;
                } catch (ClassCastException e) {
                    throw new NuxeoException(e);
                }
            }
        }
        return documents;
    }

    public void processSelectPage(String contentViewName, String listName, Boolean selection) {
        List<DocumentModel> documents = getCurrentPageDocuments(contentViewName);
        if (documents != null) {
            String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION : listName;
            if (Boolean.TRUE.equals(selection)) {
                documentsListsManager.addToWorkingList(lName, documents);
            } else {
                documentsListsManager.removeFromWorkingList(lName, documents);
            }
        }
    }

    /**
     * Handle complete table selection event after having ensured that the navigation context stills points to
     * currentDocumentRef to protect against browsers' back button errors
     */
    public void checkCurrentDocAndProcessSelectPage(String contentViewName, String listName, Boolean selection,
            String currentDocRef) {
        DocumentRef currentDocumentRef = new IdRef(currentDocRef);
        if (!currentDocumentRef.equals(navigationContext.getCurrentDocument().getRef())) {
            navigationContext.navigateToRef(currentDocumentRef);
        }
        processSelectPage(contentViewName, listName, selection);
    }

    public void processSelectRow(String docRef, String contentViewName, String listName, Boolean selection) {
        List<DocumentModel> documents = getCurrentPageDocuments(contentViewName);
        DocumentModel doc = null;
        if (documents != null) {
            for (DocumentModel pagedDoc : documents) {
                if (pagedDoc.getRef().toString().equals(docRef)) {
                    doc = pagedDoc;
                    break;
                }
            }
        }
        if (doc == null) {
            log.error(
                    String.format("could not find doc '%s' in the current page of content view page provider '%s'",
                            docRef, contentViewName));
            return;
        }
        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION : listName;
        if (Boolean.TRUE.equals(selection)) {
            documentsListsManager.addToWorkingList(lName, doc);
        } else {
            documentsListsManager.removeFromWorkingList(lName, doc);
        }
    }

    /**
     * Handle row selection event after having ensured that the navigation context stills points to currentDocumentRef
     * to protect against browsers' back button errors
     */
    public void checkCurrentDocAndProcessSelectRow(String docRef, String providerName, String listName,
            Boolean selection, String requestedCurrentDocRef) {
        DocumentRef requestedCurrentDocumentRef = new IdRef(requestedCurrentDocRef);
        DocumentRef currentDocumentRef = null;
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            currentDocumentRef = currentDocument.getRef();
        }
        if (!requestedCurrentDocumentRef.equals(currentDocumentRef)) {
            navigationContext.navigateToRef(requestedCurrentDocumentRef);
        }
        processSelectRow(docRef, providerName, listName, selection);
    }

    /**
     * Handle version row selection event after having ensured that the navigation context stills points to
     * currentDocumentRef to protect against browsers' back button errors.
     *
     * @param versionModelSelection the version model selection
     * @param requestedCurrentDocRef the requested current doc ref
     * @since 5.6
     */
    public void checkCurrentDocAndProcessVersionSelectRow(PageSelection<VersionModel> versionModelSelection,
            String requestedCurrentDocRef) {

        DocumentRef requestedCurrentDocumentRef = new IdRef(requestedCurrentDocRef);
        DocumentRef currentDocumentRef = null;
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            currentDocumentRef = currentDocument.getRef();
        }
        if (!requestedCurrentDocumentRef.equals(currentDocumentRef)) {
            navigationContext.navigateToRef(requestedCurrentDocumentRef);
        }
        processVersionSelectRow(versionModelSelection);
    }

    /**
     * Processes the version selection row.
     *
     * @param versionModelSelection the version model selection
     */
    protected final void processVersionSelectRow(PageSelection<VersionModel> versionModelSelection) {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            throw new NuxeoException("Cannot process version select row since current document is null.");
        }

        DocumentModel version = documentManager.getDocumentWithVersion(currentDocument.getRef(),
                versionModelSelection.getData());
        if (version == null) {
            throw new NuxeoException("Cannot process version select row since selected version document is null.");
        }

        if (Boolean.TRUE.equals(versionModelSelection.isSelected())) {
            documentsListsManager.addToWorkingList(DocumentsListsManager.CURRENT_VERSION_SELECTION, version);
        } else {
            documentsListsManager.removeFromWorkingList(DocumentsListsManager.CURRENT_VERSION_SELECTION, version);
        }
    }

}
