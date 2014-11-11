/*
 * (C) Copyright 2009-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.extension;

import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants;

public class ImagePlugin extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    @Override
    public String getDefaultDocType() {
        return ImagingDocumentConstants.PICTURE_TYPE_NAME;
    }

    @Override
    public boolean isOverwriteByTitle() {
        return false; // by filename
    }

}
