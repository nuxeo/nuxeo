/*
 * (C) Copyright 201' Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.security.Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.CollectionSyncRootFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link FileSystemItemFactory} for a collection synchronization root
 * {@link FolderItem}.
 *
 * @author Antoine Taillefer
 * @since 5.9.6
 */
public class CollectionSyncRootFolderItemFactory extends
        DefaultSyncRootFolderItemFactory {

    private static final Log log = LogFactory.getLog(CollectionSyncRootFolderItemFactory.class);

    public static final String FACTORY_NAME = "collectionSyncRootFolderItemFactory";

    /**
     * The factory considers that a {@link DocumentModel} is adaptable as a
     * {@link FileSystemItem} if:
     * <ul>
     * <li>It is a Collection</li>
     * <li>AND it is not HiddenInNavigation</li>
     * <li>AND it is not in the "deleted" life cycle state, unless
     * {@code includeDeleted} is true</li>
     * <li>AND it is a synchronization root registered for the current user,
     * unless {@code relaxSyncRootConstraint} is true</li>
     * </ul>
     */
    @Override
    public boolean isFileSystemItem(DocumentModel doc, boolean includeDeleted,
            boolean relaxSyncRootConstraint) throws ClientException {

        // Check Collection
        if (!Framework.getService(CollectionManager.class).isCollection(doc)) {
            log.debug(String.format(
                    "Document %s is not a Collection, it cannot be adapted as a FileSystemItem.",
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
        if (!relaxSyncRootConstraint) {
            // Check synchronization root registered for the current user
            NuxeoDriveManager nuxeoDriveManager = Framework.getLocalService(NuxeoDriveManager.class);
            Principal principal = doc.getCoreSession().getPrincipal();
            boolean isSyncRoot = nuxeoDriveManager.isSynchronizationRoot(
                    principal, doc);
            if (!isSyncRoot) {
                log.debug(String.format(
                        "Document %s is not a registered synchronization root for user %s, it cannot be adapted as a FileSystemItem.",
                        doc.getId(), principal.getName()));
                return false;
            }
        }
        return true;
    }

    @Override
    protected FileSystemItem adaptDocument(DocumentModel doc,
            boolean forceParentItem, FolderItem parentItem,
            boolean relaxSyncRootConstraint) throws ClientException {
        return new CollectionSyncRootFolderItem(name, parentItem, doc);
    }

}
