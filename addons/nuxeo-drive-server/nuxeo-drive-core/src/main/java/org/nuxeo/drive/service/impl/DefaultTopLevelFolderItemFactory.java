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

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.adapter.impl.DefaultTopLevelFolderItem;
import org.nuxeo.drive.service.TopLevelFolderItemFactory;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Default implementation of a {@link TopLevelFolderItemFactory}.
 * 
 * @author Antoine Taillefer
 */
public class DefaultTopLevelFolderItemFactory extends AbstractVirtualFolderItemFactory implements
        TopLevelFolderItemFactory {

    /*---------------------- VirtualFolderItemFactory ---------------*/
    @Override
    public FolderItem getVirtualFolderItem(Principal principal) throws ClientException {
        return getTopLevelFolderItem(principal);
    }

    /*----------------------- TopLevelFolderItemFactory ---------------------*/
    @Override
    public FolderItem getTopLevelFolderItem(Principal principal) throws ClientException {
        return new DefaultTopLevelFolderItem(getName(), principal, getFolderName());
    }

}
