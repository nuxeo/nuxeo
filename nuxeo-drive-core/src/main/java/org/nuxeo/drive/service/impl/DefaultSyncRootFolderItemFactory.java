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
import org.nuxeo.drive.adapter.impl.DefaultSyncRootFolderItem;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * Default {@link FileSystemItemFactory} for a synchronization root
 * {@link FolderItem}.
 *
 * @author Antoine Taillefer
 */
public class DefaultSyncRootFolderItemFactory extends
        AbstractFileSystemItemFactory {

    private static final Log log = LogFactory.getLog(DefaultSyncRootFolderItemFactory.class);

    /*--------------------------- AbstractFileSystemItemFactory -------------*/
    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean includeDeleted) throws ClientException {
        String userName = doc.getCoreSession().getPrincipal().getName();
        return getFileSystemItem(doc, getParentId(userName), includeDeleted);
    }

    @Override
    public void handleParameters(Map<String, String> parameters) {
        // Nothing to do as no parameters are contributed to the factory
        if (!parameters.isEmpty()) {
            throw new IllegalArgumentException(
                    "Parameter map is not empty whereas no parameters are contributed to the factory.");
        }
        log.debug(String.format("Factory %s has no parameters to handle.",
                getName()));
    }

    /**
     * The default synchronization root factory considers that a
     * {@link DocumentModel} is adaptable as a {@link FileSystemItem} if:
     * <ul>
     * <li>It is Folderish</li>
     * <li>AND it is not a version nor a proxy</li>
     * <li>AND it is not HiddenInNavigation</li>
     * <li>AND it is not in the "deleted" life cycle state, unless
     * {@code includeDeleted} is true</li>
     * <li>AND it is a synchronization root registered for the current user</li>
     * </ul>
     */
    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted)
            throws ClientException {

        // Check Folderish
        if (!doc.isFolder()) {
            log.debug(String.format(
                    "Document %s is not Folderish, it cannot be adapted as a FileSystemItem.",
                    doc.getId()));
            return false;
        }
        // Check version
        if (doc.isVersion()) {
            log.debug(String.format(
                    "Document %s is a version, it cannot be adapted as a FileSystemItem.",
                    doc.getId()));
            return false;
        }
        // Check proxy
        if (doc.isProxy()) {
            log.debug(String.format(
                    "Document %s is a proxy, it cannot be adapted as a FileSystemItem.",
                    doc.getId()));
            return false;
        }
        // Check HiddenInNavigation
        if (doc.hasFacet("HiddenInNavigation")) {
            log.debug(String.format(
                    "Document %s is HiddenInNavigation, it cannot be adapted as a FileSystemItem.",
                    doc.getId()));
            return false;
        }
        // Check "deleted" life cycle state
        if (!includeDeleted
                && LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            log.debug(String.format(
                    "Document %s is in the '%s' life cycle state, it cannot be adapted as a FileSystemItem.",
                    doc.getId(), LifeCycleConstants.DELETED_STATE));
            return false;
        }
        // Check synchronization root registered for the current user
        NuxeoDriveManager nuxeoDriveManager = Framework.getLocalService(NuxeoDriveManager.class);
        Principal principal = doc.getCoreSession().getPrincipal();
        String repoName = doc.getRepositoryName();
        SynchronizationRoots syncRoots = nuxeoDriveManager.getSynchronizationRoots(
                principal).get(repoName);
        if (!syncRoots.refs.contains(doc.getRef())) {
            log.debug(String.format(
                    "Document %s is not a registered synchronization root for user %s, it cannot be adapted as a FileSystemItem.",
                    doc.getId(), principal.getName()));
            return false;
        }
        return true;
    }

    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc,
            boolean forceParentId, String parentId) throws ClientException {
        return new DefaultSyncRootFolderItem(name, parentId, doc);
    }

    /*--------------------------- Protected ---------------------------------*/
    protected String getParentId(String userName) throws ClientException {
        FileSystemItemAdapterService fileSystemItemAdapterService = Framework.getLocalService(FileSystemItemAdapterService.class);
        return fileSystemItemAdapterService.getTopLevelFolderItemFactory().getTopLevelFolderItem(
                userName).getId();
    }

}
