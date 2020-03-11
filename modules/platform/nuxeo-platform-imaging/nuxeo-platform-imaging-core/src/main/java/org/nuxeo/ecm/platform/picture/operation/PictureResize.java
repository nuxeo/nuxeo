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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;

/**
 * Simple Operation to convert the size of a picture blob
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = PictureResize.ID, category = Constants.CAT_CONVERSION, label = "Resize a picture", description = "Use conversion service to resize a picture contained in a Document or a Blob", aliases = { "Picture.resize" })
public class PictureResize {

    private static final String HEIGHT = "height";

    private static final String WIDTH = "width";

    private static final String PICTURE_RESIZE_CONVERTER = "pictureResize";

    public static final String ID = "Picture.Resize";

    @Param(name = "maxWidth", required = true)
    protected int maxWidth = 0;

    @Param(name = "maxHeight", required = true)
    protected int maxHeigh = 0;

    @Context
    protected ConversionService service;

    @OperationMethod
    public Blob run(DocumentModel doc) {

        Blob pictureBlob = null;
        MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);
        if (mvp != null) {
            // picture:origin is not always set.
            PictureView pv = mvp.getView(mvp.getOrigin());
            if (pv != null) {
                pictureBlob = pv.getBlob();
            }
        }

        if (pictureBlob == null) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                pictureBlob = bh.getBlob();
            }
        }

        if (pictureBlob == null) {
            Blob blob = Blobs.createBlob("Unable to find a picture in the Document");
            blob.setFilename(doc.getName() + ".null");
            return blob;
        } else {
            return run(pictureBlob);
        }
    }

    @OperationMethod
    public Blob run(Blob blob) {

        SimpleBlobHolder bh = new SimpleBlobHolder(blob);
        Map<String, Serializable> parameters = new HashMap<>();

        parameters.put(WIDTH, maxWidth);
        parameters.put(HEIGHT, maxHeigh);

        if (!service.isConverterAvailable(PICTURE_RESIZE_CONVERTER).isAvailable()) {
            return blob;
        }

        BlobHolder result = service.convert(PICTURE_RESIZE_CONVERTER, bh, parameters);

        if (result != null) {
            return result.getBlob();
        } else {
            return Blobs.createBlob("Converter did not return any result");
        }
    }

}
