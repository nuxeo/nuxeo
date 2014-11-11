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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
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
public class DocumentActionsBean extends InputController implements
        DocumentActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentActionsBean.class);

    /**
     * @deprecated since 5.6: default layout can now be defined on the
     *             nxl:documentLayout tag
     */
    @Deprecated
    public static final String DEFAULT_SUMMARY_LAYOUT = "default_summary_layout";

    public static final String LIFE_CYCLE_TRANSITION_KEY = "lifeCycleTransition";

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

    /**
     * Boolean request parameter used to restore current tabs (current tab and
     * subtab) after edition.
     * <p>
     * This is useful when editing the document from a layout toggled to edit
     * mode from summary-like page.
     *
     * @since 5.6
     */
    @RequestParameter
    protected Boolean restoreCurrentTabs;

    @In(create = true)
    protected transient WebActions webActions;

    protected String comment;

    @Deprecated
    @Override
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

    @Deprecated
    @Override
    public String download() throws ClientException {
        try {
            if (fileFieldFullName == null) {
                return null;
            }

            String[] s = fileFieldFullName.split(":");
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            Blob blob = (Blob) currentDocument.getProperty(s[0], s[1]);
            String filename = getFileName(currentDocument);
            FacesContext context = FacesContext.getCurrentInstance();
            return ComponentUtils.download(context, blob, filename);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
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

    @Deprecated
    @Override
    public String downloadFromList() throws ClientException {
        return null;
    }

    @Override
    public String updateDocument(DocumentModel doc, Boolean restoreCurrentTabs)
            throws ClientException {
        try {
            String tabId = null;
            String subTabId = null;
            boolean restoreTabs = Boolean.TRUE.equals(restoreCurrentTabs);
            if (restoreTabs) {
                // save current tabs
                tabId = webActions.getCurrentTabId();
                subTabId = webActions.getCurrentSubTabId();
            }
            Events.instance().raiseEvent(EventNames.BEFORE_DOCUMENT_CHANGED,
                    doc);
            doc = documentManager.saveDocument(doc);
            throwUpdateComments(doc);
            documentManager.save();
            // some changes (versioning) happened server-side, fetch new one
            navigationContext.invalidateCurrentDocument();
            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get("document_modified"),
                    resourcesAccessor.getMessages().get(doc.getType()));
            EventManager.raiseEventsOnDocumentChange(doc);
            String res = navigationContext.navigateToDocument(doc, "after-edit");
            if (restoreTabs) {
                // restore previously stored tabs;
                webActions.setCurrentTabId(tabId);
                webActions.setCurrentSubTabId(subTabId);
            }
            return res;
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    // kept for BBB
    protected String updateDocument(DocumentModel doc) throws ClientException {
        return updateDocument(doc, restoreCurrentTabs);
    }

    @Override
    public String updateCurrentDocument() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return updateDocument(currentDocument);
    }

    @Deprecated
    @Override
    public String updateDocument() throws ClientException {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        return updateDocument(changeableDocument);
    }

    @Override
    public String updateDocumentAsNewVersion() throws ClientException {
        try {
            DocumentModel changeableDocument = navigationContext.getChangeableDocument();
            changeableDocument.putContextData(
                    org.nuxeo.common.collections.ScopeType.REQUEST,
                    VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY,
                    Boolean.TRUE);
            changeableDocument = documentManager.saveDocument(changeableDocument);

            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get("new_version_created"));
            // then follow the standard pageflow for edited documents
            EventManager.raiseEventsOnDocumentChange(changeableDocument);
            return navigationContext.navigateToDocument(changeableDocument,
                    "after-edit");
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public String createDocument() throws ClientException {
        Type docType = typesTool.getSelectedType();
        return createDocument(docType.getId());
    }

    @Override
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

    @Override
    public String saveDocument() throws ClientException {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        return saveDocument(changeableDocument);
    }

    @RequestParameter
    protected String parentDocumentPath;

    @Override
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
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
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
            facesMessages.add(StatusMessage.Severity.INFO,
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

    @Override
    public boolean getWriteRight() throws ClientException {
        // TODO: WRITE is a high level compound permission (i.e. more like a
        // user profile), public methods of the Nuxeo framework should only
        // check atomic / specific permissions such as WRITE_PROPERTIES,
        // REMOVE, ADD_CHILDREN depending on the action to execute instead
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

        if (!(docList == null || docList.isEmpty())
                && deleteActions.checkDeletePermOnParents(docList)) {
            for (DocumentModel document : docList) {
                if (document.hasFacet(FacetNames.PUBLISH_SPACE)
                        || document.hasFacet(FacetNames.MASTER_PUBLISH_SPACE)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
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
