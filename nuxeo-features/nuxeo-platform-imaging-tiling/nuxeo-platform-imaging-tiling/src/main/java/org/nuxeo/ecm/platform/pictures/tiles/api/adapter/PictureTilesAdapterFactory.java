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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Factory method for the DocumentModelAdapter for PictureTiles Contains the
 * logic to choose the correct implementation according to DocumentModel
 *
 * @author tiry
 *
 */
public class PictureTilesAdapterFactory implements DocumentAdapterFactory {

    private static final Log log = LogFactory.getLog(PictureTilesAdapterFactory.class);

    public Object getAdapter(DocumentModel doc, Class itf) {
        PictureTilingService tilingService = null;
        String blobProperty = null;
        try {
            tilingService = Framework.getService(PictureTilingService.class);
            blobProperty = tilingService.getBlobProperty(doc.getType());
        } catch (Exception e) {
            log.error("Unable to load PictureTilingService", e);
        }

        PictureTilesAdapter ptAdapter;
        try {
            ptAdapter = getAdapterFor(doc, blobProperty);

            if (ptAdapter != null) {
                return ptAdapter;
            }

            // else fall back on default
            if (doc.hasSchema("file")) {
                Blob blob = (Blob) doc.getProperty("file", "content");
                if (blob != null) {
                    PictureTilesAdapter adapter = new PictureTilesAdapterImpl(
                            doc, "file:content");
                    adapter.setFileName((String) doc.getProperty("file",
                            "filename"));
                    return adapter;
                }
            } else {
                return new PictureTilesAdapterImpl(doc);
            }
        } catch (ClientException e) {
            log.error("Unable to get adapter", e);
        }
        return null;
    }

    private PictureTilesAdapter getAdapterFor(DocumentModel doc,
            String blobProperty) throws ClientException {
        if (blobProperty != null) {
            Blob blob = null;
            try {
                blob = (Blob) doc.getPropertyValue(blobProperty);
                if (blob != null) {
                    PictureTilesAdapter adapter = new PictureTilesAdapterImpl(
                            doc, blobProperty);
                    adapter.setFileName(blob.getFilename());
                    return adapter;
                }
            } catch (PropertyException e) {
                log.error("No such blob property: " + blobProperty, e);
            }
        }
        return null;
    }

}
