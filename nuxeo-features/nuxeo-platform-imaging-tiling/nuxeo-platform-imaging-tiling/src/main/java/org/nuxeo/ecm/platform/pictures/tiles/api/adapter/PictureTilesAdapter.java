/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.api.adapter;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;

/**
 * Interface for the DocumentModelAdapter that gives access to the PictureTiles
 * of the underlying image.
 *
 * @author tiry
 */
public interface PictureTilesAdapter {

    PictureTiles getTiles(int tileWidth, int tileHeight, int maxTiles)
            throws ClientException;

    void setXPath(String path);

    void setDoc(DocumentModel doc);

    void setFileName(String fileName);

    void cleanup();
}
