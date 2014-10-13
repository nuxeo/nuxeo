/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.seam;

import java.io.File;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceHelper;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.security.SecurityException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.user.center.UserCenterViewManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7
 */
@Name("nuxeoDriveActions")
@Scope(ScopeType.PAGE)
@Install(precedence = Install.FRAMEWORK)
public class NuxeoDriveActions extends InputController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(NuxeoDriveActions.class);

    protected static final String IS_UNDER_SYNCHRONIZATION_ROOT = "nuxeoDriveIsUnderSynchronizationRoot";

    protected static final String CURRENT_SYNCHRONIZATION_ROOT = "nuxeoDriveCurrentSynchronizationRoot";

    public static final String NXDRIVE_PROTOCOL = "nxdrive";

    public static final String PROTOCOL_COMMAND_EDIT = "edit";

    public static final String UPDATE_SITE_URL_PROP_KEY = "org.nuxeo.drive.update.site.url";

    public static final String SERVER_VERSION_PROP_KEY = "org.nuxeo.ecm.product.version";

    public static final String DESKTOP_PACKAGE_URL_LATEST_SEGMENT = "latest";

    public static final String DESKTOP_PACKAGE_PREFIX = "nuxeo-drive.";

    public static final String MSI_EXTENSION = "msi";

    public static final String DMG_EXTENSION = "dmg";

    public static final String WINDOWS_PLATFORM = "windows";

    public static final String OSX_PLATFORM = "osx";

    private static final String MODE_DRIVE_VIEW = "view_drive_metadata";

    protected FileSystemItem currentFileSystemItem;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient UserCenterViewManager userCenterViews;

    @Factory(value = CURRENT_SYNCHRONIZATION_ROOT, scope = ScopeType.EVENT)
    public DocumentModel getCurrentSynchronizationRoot() throws ClientException {
        // Use the event context as request cache
        Context cache = Contexts.getEventContext();
        Boolean isUnderSync = (Boolean) cache.get(IS_UNDER_SYNCHRONIZATION_ROOT);
        if (isUnderSync == null) {
            NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
            Set<IdRef> references = driveManager.getSynchronizationRootReferences(documentManager);
            DocumentModelList path = navigationContext.getCurrentPath();
            DocumentModel root = null;
            // list is ordered such as closest synchronized ancestor is
            // considered the current synchronization root
            for (DocumentModel parent : path) {
                if (references.contains(parent.getRef())) {
                    root = parent;
                    break;
                }
            }
            cache.set(CURRENT_SYNCHRONIZATION_ROOT, root);
            cache.set(IS_UNDER_SYNCHRONIZATION_ROOT, root != null);
        }
        return (DocumentModel) cache.get(CURRENT_SYNCHRONIZATION_ROOT);
    }

    @Factory(value = "canEditCurrentDocument")
    public boolean canEditCurrentDocument() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        if (currentDocument.isFolder()) {
            return false;
        }
        // Check if current document can be adapted as a FileSystemItem
        return getCurrentFileSystemItem(false) != null;
    }

    /**
     * {@link #NXDRIVE_PROTOCOL} must be handled by a protocol handler
     * configured on the client side (either on the browser, or on the OS).
     *
     * @return Drive edit URL in the form "{@link #NXDRIVE_PROTOCOL}://
     *         {@link #PROTOCOL_COMMAND_EDIT}
     *         /protocol/server[:port]/webappName/repo/repoName/nxdocid/docId/
     *         filename/fileName" for a non synchronized document or "
     *         {@link #NXDRIVE_PROTOCOL}://{@link #PROTOCOL_COMMAND_EDIT}
     *         /protocol/server[:port]/webappName/nxdoc/repoName/docRef" for a
     *         synchronized document.
     *
     * @throws ClientException
     *
     */
    public String getDriveEditURL() throws ClientException {
        // TODO NXP-15397: handle Drive not started exception
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        StringBuilder sb = new StringBuilder();
        ServletRequest servletRequest = (ServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String baseURL = VirtualHostHelper.getBaseURL(servletRequest);
        sb.append(NXDRIVE_PROTOCOL).append("://");
        sb.append(PROTOCOL_COMMAND_EDIT).append("/");
        sb.append(baseURL.replaceFirst("://", "/"));
        currentFileSystemItem = null;
        if (getCurrentFileSystemItem(true) != null) {
            log.trace(String.format(
                    "Document %s (%s) is adaptable as a FileSystemItem and under a synchronization root, let's generate the URL to open the locally synchronized file.",
                    currentDoc.getPathAsString(), currentDoc.getId()));
            String fsItemId = currentFileSystemItem.getId();
            sb.append("fsitem/");
            sb.append(fsItemId);
        } else if (getCurrentFileSystemItem(false) != null) {
            log.trace(String.format(
                    "Document %s (%s) is adaptable as a FileSystemItem but not under a synchronization root, let's generate the URL to download the file to the 'Locally Edited' folder, open it and add it to the 'Locally Edited' collection.",
                    currentDoc.getPathAsString(), currentDoc.getId()));
            String docId = navigationContext.getCurrentDocument().getId();
            String fileName = navigationContext.getCurrentDocument().getAdapter(
                    BlobHolder.class).getBlob().getFilename();
            sb.append("repo/");
            sb.append(documentManager.getRepositoryName());
            sb.append("/nxdocid/");
            sb.append(docId);
            sb.append("/filename/");
            String escapedFilename = fileName.replaceAll(
                    "(/|\\\\|\\*|<|>|\\?|\"|:|\\|)", "-");
            sb.append(URIUtils.quoteURIPathComponent(escapedFilename, true));
        } else {
            throw new ClientException(
                    String.format(
                            "Document %s (%s) is not adaptable as a FileSystemItem thus not Drive editable, \"driveEdit\" action should not be displayed.",
                            navigationContext.getCurrentDocument().getId(),
                            navigationContext.getCurrentDocument().getPathAsString()));
        }
        return sb.toString();
    }

    @Factory(value = "canSynchronizeCurrentDocument")
    public boolean canSynchronizeCurrentDocument() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        return isSyncRootCandidate(currentDocument)
                && getCurrentSynchronizationRoot() == null;
    }

    @Factory(value = "canUnSynchronizeCurrentDocument")
    public boolean canUnSynchronizeCurrentDocument() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        if (!isSyncRootCandidate(currentDocument)) {
            return false;
        }
        DocumentRef currentDocRef = currentDocument.getRef();
        DocumentModel currentSyncRoot = getCurrentSynchronizationRoot();
        if (currentSyncRoot == null) {
            return false;
        }
        return currentDocRef.equals(currentSyncRoot.getRef());
    }

    @Factory(value = "canNavigateToCurrentSynchronizationRoot")
    public boolean canNavigateToCurrentSynchronizationRoot()
            throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        if (LifeCycleConstants.DELETED_STATE.equals(currentDocument.getCurrentLifeCycleState())) {
            return false;
        }
        DocumentRef currentDocRef = currentDocument.getRef();
        DocumentModel currentSyncRoot = getCurrentSynchronizationRoot();
        if (currentSyncRoot == null) {
            return false;
        }
        return !currentDocRef.equals(currentSyncRoot.getRef());
    }

    @Factory(value = "currentDocumentUserWorkspace", scope = ScopeType.PAGE)
    public boolean isCurrentDocumentUserWorkspace() throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        return UserWorkspaceHelper.isUserWorkspace(currentDocument);
    }

    public String synchronizeCurrentDocument() throws ClientException,
            SecurityException {
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        Principal principal = documentManager.getPrincipal();
        String userName = principal.getName();
        DocumentModel newSyncRoot = navigationContext.getCurrentDocument();
        driveManager.registerSynchronizationRoot(principal, newSyncRoot,
                documentManager);
        TokenAuthenticationService tokenService = Framework.getLocalService(TokenAuthenticationService.class);
        boolean hasOneNuxeoDriveToken = false;
        for (DocumentModel token : tokenService.getTokenBindings(userName)) {
            if ("Nuxeo Drive".equals(token.getPropertyValue("authtoken:applicationName"))) {
                hasOneNuxeoDriveToken = true;
                break;
            }
        }
        if (hasOneNuxeoDriveToken) {
            return null;
        } else {
            // redirect to user center
            userCenterViews.setCurrentViewId("userCenterNuxeoDrive");
            return "view_home";
        }
    }

    public void unsynchronizeCurrentDocument() throws ClientException {
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        Principal principal = documentManager.getPrincipal();
        DocumentModel syncRoot = navigationContext.getCurrentDocument();
        driveManager.unregisterSynchronizationRoot(principal, syncRoot,
                documentManager);
    }

    public String navigateToCurrentSynchronizationRoot() throws ClientException {
        DocumentModel currentRoot = getCurrentSynchronizationRoot();
        if (currentRoot == null) {
            return "";
        }
        return navigationContext.navigateToDocument(currentRoot);
    }

    public DocumentModelList getSynchronizationRoots() throws ClientException {
        DocumentModelList syncRoots = new DocumentModelListImpl();
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        Set<IdRef> syncRootRefs = driveManager.getSynchronizationRootReferences(documentManager);
        for (IdRef syncRootRef : syncRootRefs) {
            syncRoots.add(documentManager.getDocument(syncRootRef));
        }
        return syncRoots;
    }

    public void unsynchronizeRoot(DocumentModel syncRoot)
            throws ClientException {
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        Principal principal = documentManager.getPrincipal();
        driveManager.unregisterSynchronizationRoot(principal, syncRoot,
                documentManager);
    }

    @Factory(value = "nuxeoDriveClientPackages", scope = ScopeType.CONVERSATION)
    public List<DesktopPackageDefinition> getClientPackages() {
        List<DesktopPackageDefinition> packages = new ArrayList<DesktopPackageDefinition>();
        Object desktopPackageBaseURL = Component.getInstance(
                "desktopPackageBaseURL", ScopeType.APPLICATION);
        // Add link to packages from the update site
        if (desktopPackageBaseURL != ObjectUtils.NULL) {
            // Mac OS X
            String packageName = DESKTOP_PACKAGE_PREFIX + DMG_EXTENSION;
            String packageURL = desktopPackageBaseURL + packageName;
            packages.add(new DesktopPackageDefinition(packageURL, packageName,
                    OSX_PLATFORM));
            log.debug(String.format(
                    "Added %s to the list of desktop packages available for download.",
                    packageURL));
            // Windows
            packageName = DESKTOP_PACKAGE_PREFIX + MSI_EXTENSION;
            packageURL = desktopPackageBaseURL + packageName;
            packages.add(new DesktopPackageDefinition(packageURL, packageName,
                    WINDOWS_PLATFORM));
            log.debug(String.format(
                    "Added %s to the list of desktop packages available for download.",
                    packageURL));
        }
        // Debian / Ubuntu
        // TODO: remove when Debian package is available
        packages.add(new DesktopPackageDefinition(
                "https://github.com/nuxeo/nuxeo-drive/#ubuntudebian-and-other-linux-variants-client",
                "user.center.nuxeoDrive.platform.ubuntu.docLinkTitle", "ubuntu"));
        return packages;
    }

    @Factory(value = "desktopPackageBaseURL", scope = ScopeType.APPLICATION)
    public Object getDesktopPackageBaseURL() {
        String URL = Framework.getProperty(UPDATE_SITE_URL_PROP_KEY);
        if (URL == null) {
            return ObjectUtils.NULL;
        }
        StringBuilder sb = new StringBuilder(URL);
        if (!URL.endsWith("/")) {
            sb.append("/");
        }
        sb.append(DESKTOP_PACKAGE_URL_LATEST_SEGMENT);
        sb.append("/");
        sb.append(Framework.getProperty(SERVER_VERSION_PROP_KEY));
        sb.append("/");
        return sb.toString();
    }

    public String downloadClientPackage(String name, File file) {
        FacesContext facesCtx = FacesContext.getCurrentInstance();
        return ComponentUtils.downloadFile(facesCtx, name, file);
    }

    protected boolean isSyncRootCandidate(DocumentModel doc)
            throws ClientException {
        if (!doc.isFolder()) {
            return false;
        }
        if (LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            return false;
        }
        if (!documentManager.hasPermission(doc.getRef(),
                SecurityConstants.ADD_CHILDREN)) {
            return false;
        }
        return true;
    }

    protected FileSystemItem getCurrentFileSystemItem(boolean computePath)
            throws ClientException {
        if (currentFileSystemItem == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (computePath) {
                try {
                    currentFileSystemItem = Framework.getLocalService(
                            FileSystemItemAdapterService.class).getFileSystemItem(
                            currentDocument);
                } catch (RootlessItemException e) {
                    log.trace(String.format(
                            "Document %s (%s) is not adaptable as a FileSystemItem without forcing the parent FileSystemItem.",
                            currentDocument.getId(),
                            currentDocument.getPathAsString()));
                }
            } else {
                // Force parentItem to null to avoid computing ancestors
                currentFileSystemItem = Framework.getLocalService(
                        FileSystemItemAdapterService.class).getFileSystemItem(
                        currentDocument, null);
                if (currentFileSystemItem == null) {
                    log.trace(String.format(
                            "Document %s (%s) is not adaptable as a FileSystemItem even by forcing the parent FileSystemItem.",
                            currentDocument.getId(),
                            currentDocument.getPathAsString()));
                }
            }
        }
        return currentFileSystemItem;
    }

    /**
     * Update document model and redirect to drive view.
     */
    public String updateCurrentDocument() throws ClientException {
        DocumentModel doc = navigationContext.getCurrentDocument();
        try {
            Events.instance().raiseEvent(EventNames.BEFORE_DOCUMENT_CHANGED,
                    doc);
            doc = documentManager.saveDocument(doc);
            // throwUpdateComments(doc);
            documentManager.save();
            // some changes (versioning) happened server-side, fetch new one
            navigationContext.invalidateCurrentDocument();
            facesMessages.add(StatusMessage.Severity.INFO,
                    resourcesAccessor.getMessages().get("document_modified"),
                    resourcesAccessor.getMessages().get(doc.getType()));
            EventManager.raiseEventsOnDocumentChange(doc);

            Events.instance().raiseEvent(
                    org.nuxeo.ecm.webapp.helpers.EventNames.NAVIGATE_TO_DOCUMENT,
                    doc);

            return MODE_DRIVE_VIEW;

        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

}
