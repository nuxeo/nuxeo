/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.picture.operation;

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
 * This operation is needed because using the default blob operation is too complicated in the case of the Picture
 * DocumentType.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = GetPictureView.ID, category = Constants.CAT_CONVERSION, label = "Get image view", description = "Get an image from a Picture document.", aliases = { "Picture.getView" })
public class GetPictureView {

    public static final String ID = "Picture.GetView";

    @Param(name = "viewName", required = false)
    protected String viewName;

    @OperationMethod
    public Blob run(DocumentModel doc) {

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

        return pv.getBlob();
    }

}
