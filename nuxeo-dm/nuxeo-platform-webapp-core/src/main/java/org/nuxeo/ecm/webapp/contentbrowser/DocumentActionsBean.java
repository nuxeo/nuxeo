/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.validation.DocumentValidationException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.codec.DocumentFileCodec;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.action.ActionContextProvider;
import org.nuxeo.ecm.webapp.action.DeleteActions;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Handles creation and edition of a document.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 * @author M.-A. Darche
 */
@Name("documentActions")
@Scope(CONVERSATION)
public class DocumentActionsBean extends InputController implements DocumentActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentActionsBean.class);

    /**
     * @deprecated since 5.6: default layout can now be defined on the nxl:documentLayout tag
     */
    @Deprecated
    public static final String DEFAULT_SUMMARY_LAYOUT = "default_summary_layout";

    public static final String LIFE_CYCLE_TRANSITION_KEY = "lifeCycleTransition";

    public static final String BLOB_ACTIONS_CATEGORY = "BLOB_ACTIONS";

    @In(create = true)
    protected transient NavigationContext navigationContext;

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

    @In(create = true, required = false)
    protected transient ActionContextProvider actionContextProvider;

    /**
     * Boolean request parameter used to restore current tabs (current tab and subtab) after edition.
     * <p>
     * This is useful when editing the document from a layout toggled to edit mode from summary-like page.
     *
     * @since 5.6
     */
    @RequestParameter
    protected Boolean restoreCurrentTabs;

    @In(create = true)
    protected transient WebActions webActions;

    protected String comment;

    @In(create = true)
    protected Map<String, String> messages;

    @Deprecated
    @Override
    @Factory(autoCreate = true, value = "currentDocumentSummaryLayout", scope = EVENT)
    public String getCurrentDocumentSummaryLayout() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null) {
            return null;
        }
        String[] layouts = typeManager.getType(doc.getType()).getLayouts(BuiltinModes.SUMMARY, null);

        if (layouts != null && layouts.length > 0) {
            return layouts[0];
        }
        return DEFAULT_SUMMARY_LAYOUT;
    }

    @Override
    @Factory(autoCreate = true, value = "currentDocumentType", scope = EVENT)
    public Type getCurrentType() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null) {
            return null;
        }
        return typeManager.getType(doc.getType());
    }

    @Override
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

    @Deprecated
    @Override
    public String editDocument() {
        navigationContext.setChangeableDocument(navigationContext.getCurrentDocument());
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument(), "edit");
    }

    public String getFileName(DocumentModel doc) {
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
    }

    @Override
    public void download(DocumentView docView) {
        if (docView == null) {
            return;
        }
        DocumentLocation docLoc = docView.getDocumentLocation();
        // fix for NXP-1799
        if (documentManager == null) {
            RepositoryLocation loc = new RepositoryLocation(docLoc.getServerName());
            navigationContext.setCurrentServerLocation(loc);
            documentManager = navigationContext.getOrCreateDocumentManager();
        }
        DocumentModel doc = documentManager.getDocument(docLoc.getDocRef());
        if (doc == null) {
            return;
        }
        String xpath = docView.getParameter(DocumentFileCodec.FILE_PROPERTY_PATH_KEY);
        DownloadService downloadService = Framework.getService(DownloadService.class);
        Blob blob = downloadService.resolveBlob(doc, xpath);
        if (blob == null) {
            log.warn("No blob for docView: " + docView);
            return;
        }
        // get properties from document view
        String filename = DocumentFileCodec.getFilename(doc, docView);

        if (blob.getLength() > Functions.getBigFileSizeLimit()) {
            FacesContext context = FacesContext.getCurrentInstance();
            String bigDownloadURL = BaseURL.getBaseURL() + "/" + downloadService.getDownloadUrl(doc, xpath, filename);
            try {
                context.getExternalContext().redirect(bigDownloadURL);
            } catch (IOException e) {
                log.error("Error while redirecting for big file downloader", e);
            }
        } else {
            ComponentUtils.download(doc, xpath, blob, filename, "download");
        }
    }

    @Deprecated
    @Override
    public String downloadFromList() {
        return null;
    }

    @Override
    public String updateDocument(DocumentModel doc, Boolean restoreCurrentTabs) {
        String tabId = null;
        String subTabId = null;
        boolean restoreTabs = Boolean.TRUE.equals(restoreCurrentTabs);
        if (restoreTabs) {
            // save current tabs
            tabId = webActions.getCurrentTabId();
            subTabId = webActions.getCurrentSubTabId();
        }
        Events.instance().raiseEvent(EventNames.BEFORE_DOCUMENT_CHANGED, doc);
        try {
            doc = documentManager.saveDocument(doc);
        } catch (DocumentValidationException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.schema.constraint.violation.documentValidation"), e.getMessage());
            return null;
        }

        throwUpdateComments(doc);
        documentManager.save();
        // some changes (versioning) happened server-side, fetch new one
        navigationContext.invalidateCurrentDocument();
        facesMessages.add(StatusMessage.Severity.INFO, messages.get("document_modified"), messages.get(doc.getType()));
        EventManager.raiseEventsOnDocumentChange(doc);
        String res = navigationContext.navigateToDocument(doc, "after-edit");
        if (restoreTabs) {
            // restore previously stored tabs;
            webActions.setCurrentTabId(tabId);
            webActions.setCurrentSubTabId(subTabId);
        }
        return res;
    }

    // kept for BBB
    protected String updateDocument(DocumentModel doc) {
        return updateDocument(doc, restoreCurrentTabs);
    }

    @Override
    public String updateCurrentDocument() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return updateDocument(currentDocument);
    }

    @Deprecated
    @Override
    public String updateDocument() {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        return updateDocument(changeableDocument);
    }

    @Override
    public String updateDocumentAsNewVersion() {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        changeableDocument.putContextData(org.nuxeo.common.collections.ScopeType.REQUEST,
                VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, Boolean.TRUE);
        changeableDocument = documentManager.saveDocument(changeableDocument);

        facesMessages.add(StatusMessage.Severity.INFO, messages.get("new_version_created"));
        // then follow the standard pageflow for edited documents
        EventManager.raiseEventsOnDocumentChange(changeableDocument);
        return navigationContext.navigateToDocument(changeableDocument, "after-edit");
    }

    @Override
    public String createDocument() {
        Type docType = typesTool.getSelectedType();
        return createDocument(docType.getId());
    }

    @Override
    public String createDocument(String typeName) {
        Type docType = typeManager.getType(typeName);
        // we cannot use typesTool as intermediary since the DataModel callback
        // will alter whatever type we set
        typesTool.setSelectedType(docType);
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(CoreEventConstants.PARENT_PATH, navigationContext.getCurrentDocument().getPathAsString());
        DocumentModel changeableDocument = documentManager.createDocumentModel(typeName, context);
        navigationContext.setChangeableDocument(changeableDocument);
        return navigationContext.getActionResult(changeableDocument, UserAction.CREATE);
    }

    @Override
    public String saveDocument() {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        return saveDocument(changeableDocument);
    }

    @RequestParameter
    protected String parentDocumentPath;

    @Override
    public String saveDocument(DocumentModel newDocument) {
        // Document has already been created if it has an id.
        // This will avoid creation of many documents if user hit create button
        // too many times.
        if (newDocument.getId() != null) {
            log.debug("Document " + newDocument.getName() + " already created");
            return navigationContext.navigateToDocument(newDocument, "after-create");
        }
        PathSegmentService pss = Framework.getService(PathSegmentService.class);
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (parentDocumentPath == null) {
            if (currentDocument == null) {
                // creating item at the root
                parentDocumentPath = documentManager.getRootDocument().getPathAsString();
            } else {
                parentDocumentPath = navigationContext.getCurrentDocument().getPathAsString();
            }
        }

        newDocument.setPathInfo(parentDocumentPath, pss.generatePathSegment(newDocument));

        try {
            newDocument = documentManager.createDocument(newDocument);
        } catch (DocumentValidationException e) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    messages.get("label.schema.constraint.violation.documentValidation"), e.getMessage());
            return null;
        }
        documentManager.save();

        logDocumentWithTitle("Created the document: ", newDocument);
        facesMessages.add(StatusMessage.Severity.INFO, messages.get("document_saved"),
                messages.get(newDocument.getType()));

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);
        return navigationContext.navigateToDocument(newDocument, "after-create");
    }

    @Override
    public boolean getWriteRight() {
        // TODO: WRITE is a high level compound permission (i.e. more like a
        // user profile), public methods of the Nuxeo framework should only
        // check atomic / specific permissions such as WRITE_PROPERTIES,
        // REMOVE, ADD_CHILDREN depending on the action to execute instead
        return documentManager.hasPermission(navigationContext.getCurrentDocument().getRef(), SecurityConstants.WRITE);
    }

    // Send the comment of the update to the Core
    private void throwUpdateComments(DocumentModel changeableDocument) {
        if (comment != null && !"".equals(comment)) {
            changeableDocument.getContextData().put("comment", comment);
        }
    }

    @Deprecated
    @Override
    public String getComment() {
        return "";
    }

    @Deprecated
    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean getCanUnpublish() {
        List<DocumentModel> docList = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);

        if (!(docList == null || docList.isEmpty()) && deleteActions.checkDeletePermOnParents(docList)) {
            for (DocumentModel document : docList) {
                if (document.hasFacet(FacetNames.PUBLISH_SPACE) || document.hasFacet(FacetNames.MASTER_PUBLISH_SPACE)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    @Observer(EventNames.BEFORE_DOCUMENT_CHANGED)
    public void followTransition(DocumentModel changedDocument) {
        String transitionToFollow = (String) changedDocument.getContextData(ScopeType.REQUEST,
                LIFE_CYCLE_TRANSITION_KEY);
        if (transitionToFollow != null) {
            documentManager.followTransition(changedDocument.getRef(), transitionToFollow);
            documentManager.save();
        }
    }

    /**
     * @since 7.3
     */
    public List<Action> getBlobActions(DocumentModel doc, String blobXPath, Blob blob) {
        ActionContext ctx = actionContextProvider.createActionContext();
        ctx.putLocalVariable("document", doc);
        ctx.putLocalVariable("blob", blob);
        ctx.putLocalVariable("blobXPath", blobXPath);
        return webActions.getActionsList(BLOB_ACTIONS_CATEGORY, ctx, true);
    }

    /**
     * @since 7.3
     */
    @WebRemote
    public List<AppLink> getAppLinks(String docId, String blobXPath) {
        DocumentRef docRef = new IdRef(docId);
        DocumentModel doc = documentManager.getDocument(docRef);
        Serializable value = doc.getPropertyValue(blobXPath);

        if (value == null || !(value instanceof ManagedBlob)) {
            return null;
        }
        ManagedBlob managedBlob = (ManagedBlob) value;

        BlobManager blobManager = Framework.getService(BlobManager.class);
        BlobProvider blobProvider = blobManager.getBlobProvider(managedBlob.getProviderId());
        if (blobProvider == null) {
            log.error("No registered blob provider for key: " + managedBlob.getKey());
            return null;
        }

        String user = documentManager.getPrincipal().getName();

        try {
            return blobProvider.getAppLinks(user, managedBlob);
        } catch (IOException e) {
            log.error("Failed to retrieve application links", e);
        }
        return null;
    }

    /**
     * Checks if the main blob can be updated by a user-initiated action.
     *
     * @since 7.10
     */
    public boolean getCanUpdateMainBlob() {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null) {
            return false;
        }
        BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
        if (blobHolder == null) {
            return false;
        }
        Blob blob = blobHolder.getBlob();
        if (blob == null) {
            return true;
        }
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob);
        if (blobProvider == null) {
            return true;
        }
        return blobProvider.supportsUserUpdate();
    }

}
