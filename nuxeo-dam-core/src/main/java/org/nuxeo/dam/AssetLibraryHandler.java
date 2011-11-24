/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.dam;

import org.nuxeo.common.utils.Path;
import org.nuxeo.dam.AssetLibrary;
import org.nuxeo.dam.Constants;
import org.nuxeo.dam.DamService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.content.template.service.PostContentCreationHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public class AssetLibraryHandler implements PostContentCreationHandler {

    public static final String DC_TITLE = "dc:title";

    public static final String DC_DESCRIPTION = "dc:description";

    @Override
    public void execute(CoreSession session) {
        try {
            DamService damService = Framework.getLocalService(DamService.class);
            AssetLibrary assetLibrary = damService.getAssetLibrary();
            if (assetLibrary != null) {
                DocumentRef docRef = new PathRef(assetLibrary.getPath());
                if (!session.exists(docRef)) {
                    Path path = new Path(assetLibrary.getPath());
                    String parentPath = path.removeLastSegments(1).toString();
                    String name = path.lastSegment();

                    DocumentModel doc = session.createDocumentModel(
                            parentPath, name, Constants.IMPORT_ROOT_TYPE);
                    doc.setPropertyValue(DC_TITLE,
                            assetLibrary.getTitle());
                    doc.setPropertyValue(DC_DESCRIPTION,
                            assetLibrary.getDescription());
                    session.createDocument(doc);
                }
            }
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

}
