/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Olivier Grisel <ogrisel@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.seam;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.drive.NuxeoDriveConstants;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.hierarchy.userworkspace.adapter.UserWorkspaceHelper;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.user.center.UserCenterViewManager;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.security.AbstractUserGroupManagement;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7
 */
@Name("nuxeoDriveActions")
@Scope(ScopeType.PAGE)
@Install(precedence = Install.FRAMEWORK)
public class NuxeoDriveActions extends InputController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(NuxeoDriveActions.class);

    /** @since 9.3 */
    public static final String NUXEO_DRIVE_APPLICATION_NAME = "Nuxeo Drive";

    protected static final String IS_UNDER_SYNCHRONIZATION_ROOT = "nuxeoDriveIsUnderSynchronizationRoot";

    protected static final String CURRENT_SYNCHRONIZATION_ROOT = "nuxeoDriveCurrentSynchronizationRoot";

    public static final String NXDRIVE_PROTOCOL = "nxdrive";

    public static final String PROTOCOL_COMMAND_EDIT = "edit";

    /**
     * @deprecated since 10.2
     */
    @Deprecated
    public static final String DESKTOP_PACKAGE_URL_LATEST_SEGMENT = "latest";

    public static final String DESKTOP_PACKAGE_PREFIX = "nuxeo-drive.";

    public static final String MSI_EXTENSION = "exe";

    public static final String DMG_EXTENSION = "dmg";

    public static final String WINDOWS_PLATFORM = "windows";

    public static final String OSX_PLATFORM = "osx";

    private static final String DRIVE_METADATA_VIEW = "view_drive_metadata";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected UserCenterViewManager userCenterViews;

    @In(create = true)
    protected DocumentActions documentActions;

    @Factory(value = CURRENT_SYNCHRONIZATION_ROOT, scope = ScopeType.EVENT)
    public DocumentModel getCurrentSynchronizationRoot() {
        // Use the event context as request cache
        Context cache = Contexts.getEventContext();
        Boolean isUnderSync = (Boolean) cache.get(IS_UNDER_SYNCHRONIZATION_ROOT);
        if (isUnderSync == null) {
            NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
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

    public boolean canEditDocument(DocumentModel doc) {
        if (doc == null || !documentManager.exists(doc.getRef())) {
            return false;
        }
        if (doc.isFolder() || doc.isProxy()) {
            return false;
        }
        if (!documentManager.hasPermission(doc.getRef(), SecurityConstants.WRITE)) {
            return false;
        }
        // Check if current document can be adapted as a FileSystemItem
        return getFileSystemItem(doc) != null;
    }

    public boolean canEditBlob(DocumentModel doc, String xPath) {
        return canEditDocument(doc) && doc.getPropertyValue(xPath) instanceof Blob;
    }

    public boolean hasOneDriveToken(NuxeoPrincipal user) throws UnsupportedEncodingException {
        TokenAuthenticationService tokenService = Framework.getService(TokenAuthenticationService.class);
        for (DocumentModel token : tokenService.getTokenBindings(user.getName())) {
            String applicationName = (String) token.getPropertyValue("authtoken:applicationName");
            if (applicationName == null) {
                continue;
            }
            // We do the URL decoding for backward compatibility reasons, but in the future token parameters should be
            // stored in their natural format (i.e. not needing re-decoding).
            if (NUXEO_DRIVE_APPLICATION_NAME.equals(URLDecoder.decode(applicationName, UTF_8.toString()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the Drive edit URL for the current document.
     *
     * @see #getDriveEditURL(DocumentModel)
     */
    public String getDriveEditURL() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getDriveEditURL(currentDocument);
    }

    /**
     * Returns the Drive edit URL for the given document.
     * <p>
     * {@link #NXDRIVE_PROTOCOL} must be handled by a protocol handler configured on the client side (either on the
     * browser, or on the OS).
     *
     * @since 7.4
     * @return Drive edit URL in the form "{@link #NXDRIVE_PROTOCOL}:// {@link #PROTOCOL_COMMAND_EDIT}
     *         /protocol/server[:port]/webappName/[user/userName/]repo/repoName/nxdocid/docId/filename/fileName[/
     *         downloadUrl/downloadUrl]"
     */
    public String getDriveEditURL(DocumentModel currentDocument) {
        if (currentDocument == null) {
            return null;
        }
        // TODO NXP-15397: handle Drive not started exception
        BlobHolder bh = currentDocument.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new NuxeoException(String.format("Document %s (%s) is not a BlobHolder, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(), currentDocument.getId()));
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            throw new NuxeoException(String.format("Document %s (%s) has no blob, cannot get Drive Edit URL.",
                    currentDocument.getPathAsString(), currentDocument.getId()));
        }
        return getDriveEditURL(currentDocument, blob, null);
    }

    public String getDriveEditURL(DocumentModel doc, String xPath) {
        if (doc == null) {
            return null;
        }

        Object obj = doc.getPropertyValue(xPath);
        if (!(obj instanceof Blob)) {
            throw new NuxeoException(
                    String.format("Property %s of document %s (%s) is not a blob, cannot get Drive Edit URL.", xPath,
                            doc.getPathAsString(), doc.getId()));
        }
        Blob blob = (Blob) obj;

        return getDriveEditURL(doc, blob, xPath);
    }

    public String getDriveEditURL(DocumentModel doc, Blob blob, String xPath) {
        if (doc == null || blob == null) {
            return null;
        }

        String editURL = "%s://%s/%suser/%s/repo/%s/nxdocid/%s/filename/%s/downloadUrl/%s";
        ServletRequest servletRequest = (ServletRequest) FacesContext.getCurrentInstance()
                                                                     .getExternalContext()
                                                                     .getRequest();
        String baseURL = VirtualHostHelper.getBaseURL(servletRequest).replaceFirst("://", "/");

        String user = documentManager.getPrincipal().getName();
        String repo = documentManager.getRepositoryName();
        String docId = doc.getId();
        String filename = blob.getFilename();
        filename = filename.replaceAll("(/|\\\\|\\*|<|>|\\?|\"|:|\\|)", "-");
        filename = URIUtils.quoteURIPathComponent(filename, true);
        DownloadService downloadService = Framework.getService(DownloadService.class);
        if (xPath == null) {
            xPath = DownloadService.BLOBHOLDER_0;
        }
        String downloadUrl = downloadService.getDownloadUrl(doc, xPath, filename);

        return String.format(editURL, NXDRIVE_PROTOCOL, PROTOCOL_COMMAND_EDIT, baseURL, user, repo, docId, filename,
                downloadUrl);
    }

    public String navigateToUserCenterNuxeoDrive() {
        return getUserCenterNuxeoDriveView();
    }

    @Factory(value = "canSynchronizeCurrentDocument")
    public boolean canSynchronizeCurrentDocument() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        return isSyncRootCandidate(currentDocument) && getCurrentSynchronizationRoot() == null;
    }

    @Factory(value = "canUnSynchronizeCurrentDocument")
    public boolean canUnSynchronizeCurrentDocument() {
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
    public boolean canNavigateToCurrentSynchronizationRoot() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        if (currentDocument.isTrashed()) {
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
    public boolean isCurrentDocumentUserWorkspace() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        return UserWorkspaceHelper.isUserWorkspace(currentDocument);
    }

    public String synchronizeCurrentDocument() throws UnsupportedEncodingException {
        NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
        NuxeoPrincipal principal = documentManager.getPrincipal();
        DocumentModel newSyncRoot = navigationContext.getCurrentDocument();
        driveManager.registerSynchronizationRoot(principal, newSyncRoot, documentManager);
        boolean hasOneNuxeoDriveToken = hasOneDriveToken(principal);
        if (hasOneNuxeoDriveToken) {
            return null;
        } else {
            // redirect to user center
            return getUserCenterNuxeoDriveView();
        }
    }

    public void unsynchronizeCurrentDocument() {
        NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
        NuxeoPrincipal principal = documentManager.getPrincipal();
        DocumentModel syncRoot = navigationContext.getCurrentDocument();
        driveManager.unregisterSynchronizationRoot(principal, syncRoot, documentManager);
    }

    public String navigateToCurrentSynchronizationRoot() {
        DocumentModel currentRoot = getCurrentSynchronizationRoot();
        if (currentRoot == null) {
            return "";
        }
        return navigationContext.navigateToDocument(currentRoot);
    }

    public DocumentModelList getSynchronizationRoots() {
        DocumentModelList syncRoots = new DocumentModelListImpl();
        NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
        Set<IdRef> syncRootRefs = driveManager.getSynchronizationRootReferences(documentManager);
        for (IdRef syncRootRef : syncRootRefs) {
            syncRoots.add(documentManager.getDocument(syncRootRef));
        }
        return syncRoots;
    }

    public void unsynchronizeRoot(DocumentModel syncRoot) {
        NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
        NuxeoPrincipal principal = documentManager.getPrincipal();
        driveManager.unregisterSynchronizationRoot(principal, syncRoot, documentManager);
    }

    @Factory(value = "nuxeoDriveClientPackages", scope = ScopeType.CONVERSATION)
    public List<DesktopPackageDefinition> getClientPackages() {
        List<DesktopPackageDefinition> packages = new ArrayList<>();
        Object desktopPackageBaseURL = Component.getInstance("desktopPackageBaseURL", ScopeType.APPLICATION);
        // Add link to packages from the update site
        if (desktopPackageBaseURL != ObjectUtils.NULL) {
            // Mac OS X
            String packageName = DESKTOP_PACKAGE_PREFIX + DMG_EXTENSION;
            String packageURL = desktopPackageBaseURL + packageName;
            packages.add(new DesktopPackageDefinition(packageURL, packageName, OSX_PLATFORM));
            log.debug("Added {} to the list of desktop packages available for download.", packageURL);
            // Windows
            packageName = DESKTOP_PACKAGE_PREFIX + MSI_EXTENSION;
            packageURL = desktopPackageBaseURL + packageName;
            packages.add(new DesktopPackageDefinition(packageURL, packageName, WINDOWS_PLATFORM));
            log.debug("Added {} to the list of desktop packages available for download.", packageURL);
        }
        // Debian / Ubuntu
        // TODO: remove when Debian package is available
        packages.add(new DesktopPackageDefinition(
                "https://github.com/nuxeo/nuxeo-drive#debian-based-distributions-and-other-gnulinux-variants-client",
                "user.center.nuxeoDrive.platform.ubuntu.docLinkTitle", "ubuntu"));
        return packages;
    }

    @Factory(value = "desktopPackageBaseURL", scope = ScopeType.APPLICATION)
    public Object getDesktopPackageBaseURL() {
        String url = Framework.getProperty(NuxeoDriveConstants.UPDATE_SITE_URL_PROP_KEY);
        if (url == null) {
            return ObjectUtils.NULL;
        }
        StringBuilder sb = new StringBuilder(url);
        if (!url.endsWith("/")) {
            sb.append("/");
        }
        return sb.toString();
    }

    protected boolean isSyncRootCandidate(DocumentModel doc) {
        return doc.isFolder() && !doc.isTrashed();
    }

    protected FileSystemItem getFileSystemItem(DocumentModel doc) {
        // Force parentItem to null to avoid computing ancestors
        // NXP-19442: Avoid useless and costly call to DocumentModel#getLockInfo
        FileSystemItem fileSystemItem = Framework.getService(FileSystemItemAdapterService.class)
                                                 .getFileSystemItem(doc, null, false, false, false);
        if (fileSystemItem == null) {
            log.debug("Document {} ({}) is not adaptable as a FileSystemItem.", doc::getPathAsString, doc::getId);
        }
        return fileSystemItem;
    }

    protected String getUserCenterNuxeoDriveView() {
        userCenterViews.setCurrentViewId("userCenterNuxeoDrive");
        return AbstractUserGroupManagement.VIEW_HOME;
    }

    /**
     * Update document model and redirect to drive view.
     */
    public String updateCurrentDocument() {
        documentActions.updateCurrentDocument();
        return DRIVE_METADATA_VIEW;
    }

}
