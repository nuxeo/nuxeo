/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.pictures.tiles.api.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;

/**
 * Interface for the DocumentModelAdapter that gives access to the PictureTiles of the underlying image.
 *
 * @author tiry
 */
public interface PictureTilesAdapter {

    PictureTiles getTiles(int tileWidth, int tileHeight, int maxTiles);

    void setXPath(String path);

    void setDoc(DocumentModel doc);

    /**
     * @deprecated since 9.1 as filename is now hold by the blob and no longer exist beside it
     */
    @Deprecated
    void setFileName(String fileName);

    void cleanup();
}
