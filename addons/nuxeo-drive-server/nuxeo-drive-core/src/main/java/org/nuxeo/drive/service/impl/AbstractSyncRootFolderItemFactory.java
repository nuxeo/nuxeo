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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.security.Principal;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Base {@link FileSystemItemFactory} for a synchronization root {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public abstract class AbstractSyncRootFolderItemFactory extends AbstractFileSystemItemFactory {

    private static final Log log = LogFactory.getLog(AbstractSyncRootFolderItemFactory.class);

    /**
     * Returns the parent {@link FileSystemItem}.
     */
    protected abstract FolderItem getParentItem(DocumentModel doc) throws ClientException;

    /**
     * No parameters by default.
     */
    @Override
    public void handleParameters(Map<String, String> parameters) throws ClientException {
        // Nothing to do as no parameters are contributed to the factory
        if (!parameters.isEmpty()) {
            throw new IllegalArgumentException(
                    "Parameter map is not empty whereas no parameters are contributed to the factory.");
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Factory %s has no parameters to handle.", getName()));
        }
    }

    /**
     * The factory considers that a {@link DocumentModel} is adaptable as a {@link FileSystemItem} if:
     * <ul>
     * <li>It is Folderish</li>
     * <li>AND it is not a version nor a proxy</li>
     * <li>AND it is not HiddenInNavigation</li>
     * <li>AND it is not in the "deleted" life cycle state, unless {@code includeDeleted} is true</li>
     * <li>AND it is a synchronization root registered for the current user, unless {@code relaxSyncRootConstraint} is
     * true</li>
     * </ul>
     */
    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint)
            throws ClientException {

        // Check Folderish
        if (!doc.isFolder()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is not Folderish, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }
        // Check version
        if (doc.isVersion()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is a version, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }
        // Check proxy
        if (doc.isProxy()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is a proxy, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }
        // Check HiddenInNavigation
        if (doc.hasFacet("HiddenInNavigation")) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is HiddenInNavigation, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }
        // Check "deleted" life cycle state
        if (!includeDeleted && LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Document %s is in the '%s' life cycle state, it cannot be adapted as a FileSystemItem.",
                        doc.getId(), LifeCycleConstants.DELETED_STATE));
            }
            return false;
        }
        if (!relaxSyncRootConstraint) {
            // Check synchronization root registered for the current user
            NuxeoDriveManager nuxeoDriveManager = Framework.getLocalService(NuxeoDriveManager.class);
            Principal principal = doc.getCoreSession().getPrincipal();
            boolean isSyncRoot = nuxeoDriveManager.isSynchronizationRoot(principal, doc);
            if (!isSyncRoot) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Document %s is not a registered synchronization root for user %s, it cannot be adapted as a FileSystemItem.",
                            doc.getId(), principal.getName()));
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Force parent using {@link #getParentItem(DocumentModel)}.
     */
    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted) throws ClientException {
        return getFileSystemItem(doc, getParentItem(doc), includeDeleted);
    }

    /**
     * Force parent using {@link #getParentItem(DocumentModel)}.
     */
    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint)
            throws ClientException {
        return getFileSystemItem(doc, getParentItem(doc), includeDeleted, relaxSyncRootConstraint);
    }

    /**
     * Force parent using {@link #getParentItem(DocumentModel)}.
     */
    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo) throws ClientException {
        return getFileSystemItem(doc, getParentItem(doc), includeDeleted, relaxSyncRootConstraint, getLockInfo);
    }

}
