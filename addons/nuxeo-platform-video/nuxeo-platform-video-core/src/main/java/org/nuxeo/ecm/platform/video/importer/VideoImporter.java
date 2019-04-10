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
 *     Peter Di Lorenzo
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.video.importer;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.service.extension.AbstractFileImporter;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * This class will create a Document of type "Video" from the uploaded file, if
 * the uploaded file matches any of the mime types listed in the
 * filemanager-plugins.xml file.
 * <p>
 * If an existing document with the same title is found, it will overwrite it
 * and increment the version number if the overwrite flag is set to true;
 * Otherwise, it will generate a new title and create a new Document of type
 * Video with that title.
 */
public class VideoImporter extends AbstractFileImporter {

    private static final long serialVersionUID = 1L;

    @Override
    public String getDefaultDocType() {
        return VideoConstants.VIDEO_TYPE;
    }

    @Override
    public boolean isOverwriteByTitle() {
        return true;
    }

    @Override
    public void updateDocument(DocumentModel doc, Blob content)
            throws ClientException {
        super.updateDocument(doc, content);
        // update the icon
        Type type = Framework.getLocalService(TypeManager.class).getType(
                doc.getType());
        if (type != null) {
            doc.setProperty("common", "icon", type.getIcon());
        }
    }

}
