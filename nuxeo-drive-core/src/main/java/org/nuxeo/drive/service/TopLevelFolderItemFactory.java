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
package org.nuxeo.drive.service;

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Interface for the classes contributed to the
 * {@code topLevelFolderItemFactory} extension point of the
 * {@link FileSystemItemAdapterService}.
 * <p>
 * Allows to get the top level {@link FolderItem} and the id of the
 * synchronization root parent {@link FolderItem}.
 *
 * @author Antoine Taillefer
 * @see DefaultTopLevelFolderItemFactory
 */
public interface TopLevelFolderItemFactory {

    FolderItem getTopLevelFolderItem(String userName) throws ClientException;

    String getSyncRootParentFolderItemId(String userName)
            throws ClientException;

}
