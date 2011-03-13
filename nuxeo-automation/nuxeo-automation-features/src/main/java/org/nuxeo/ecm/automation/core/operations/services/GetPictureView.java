/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.core.operations.services;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;

/**
 * Simple Operation to extract an image view from a Picture Document.
 * <p>
 * This operation is needed because using the default blob operation
 * is too complicated in the case of the Picture DocumentType.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = GetPictureView.ID, category = Constants.CAT_CONVERSION, label = "Get image view",
        description = "Get an image from a Picture document.")
public class GetPictureView {

    public static final String ID = "Picture.getView";

    @Param(name = "viewName", required = false)
    protected String viewName;

    @OperationMethod
    public Blob run(DocumentModel doc) throws Exception {

        MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);

        if (mvp == null) {
            return null;
        }

        if (viewName == null) {
            viewName = mvp.getOrigin();
        }

        PictureView pv = mvp.getView(viewName);

        if (pv == null) {
            return null;
        }

        return (Blob) pv.getContent();
    }

}
