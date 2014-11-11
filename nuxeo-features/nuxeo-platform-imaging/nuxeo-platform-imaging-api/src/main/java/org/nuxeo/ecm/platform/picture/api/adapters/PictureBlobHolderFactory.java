/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.picture.api.adapters;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderFactory;

public class PictureBlobHolderFactory implements BlobHolderFactory {

    @Override
    public BlobHolder getBlobHolder(DocumentModel doc) {
        String docType = doc.getType();
        BlobHolder blobHolder;

        if (doc.hasFacet("Picture")) {
            blobHolder = new PictureBlobHolder(doc, "file:content");
        } else if (docType.equals("PictureBook")) {
            blobHolder = new PictureBookBlobHolder(doc, "");
        } else {
            blobHolder = null;
        }
        return blobHolder;
    }

}
