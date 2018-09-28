/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.DocumentModel;
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
    protected abstract FolderItem getParentItem(DocumentModel doc);

    /**
     * No parameters by default.
     */
    @Override
    public void handleParameters(Map<String, String> parameters) {
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
     * <li>AND it is not in the trash, unless {@code includeDeleted} is true</li>
     * <li>AND it is a synchronization root registered for the current user, unless {@code relaxSyncRootConstraint} is
     * true</li>
     * </ul>
     */
    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint) {

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
        // Check if document is in the trash
        if (!includeDeleted && doc.isTrashed()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Document %s is in the trash, it cannot be adapted as a FileSystemItem.",
                        doc.getId()));
            }
            return false;
        }
        if (!relaxSyncRootConstraint) {
            // Check synchronization root registered for the current user
            NuxeoDriveManager nuxeoDriveManager = Framework.getService(NuxeoDriveManager.class);
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
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted) {
        return getFileSystemItem(doc, getParentItem(doc), includeDeleted);
    }

    /**
     * Force parent using {@link #getParentItem(DocumentModel)}.
     */
    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) {
        return getFileSystemItem(doc, getParentItem(doc), includeDeleted, relaxSyncRootConstraint);
    }

    /**
     * Force parent using {@link #getParentItem(DocumentModel)}.
     */
    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, boolean includeDeleted, boolean relaxSyncRootConstraint,
            boolean getLockInfo) {
        return getFileSystemItem(doc, getParentItem(doc), includeDeleted, relaxSyncRootConstraint, getLockInfo);
    }

}
