/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;

/**
 * Simple Operation to convert the size of a picture blob
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = PictureResize.ID, category = Constants.CAT_CONVERSION, label = "Resize a picture", description = "Use conversion servcie to resize a picture contained in a Document or a Blob")
public class PictureResize {

    private static final String HEIGHT = "height";

    private static final String WIDTH = "width";

    private static final String PICTURE_RESIZE_CONVERTER = "pictureResize";

    public static final String ID = "Picture.resize";

    @Param(name = "maxWidth", required = true)
    protected int maxWidth = 0;

    @Param(name = "maxHeight", required = true)
    protected int maxHeigh = 0;

    @Context
    protected ConversionService service;

    @OperationMethod
    public Blob run(DocumentModel doc) throws Exception {

        Blob pictureBlob = null;
        MultiviewPicture mvp = doc.getAdapter(MultiviewPicture.class);
        if (mvp != null) {
            pictureBlob = (Blob) mvp.getView(mvp.getOrigin()).getContent();
        } else {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                pictureBlob = bh.getBlob();
            }
        }

        if (pictureBlob == null) {
            Blob blob = new StringBlob(
                    "Unable to find a picture in the Document");
            blob.setMimeType("text/plain");
            blob.setFilename(doc.getName() + ".null");
            return blob;
        } else {
            return run(pictureBlob);
        }
    }

    @OperationMethod
    public Blob run(Blob blob) throws Exception {

        SimpleBlobHolder bh = new SimpleBlobHolder(blob);
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();

        parameters.put(WIDTH, maxWidth);
        parameters.put(HEIGHT, maxHeigh);

        if (!service.isConverterAvailable(PICTURE_RESIZE_CONVERTER).isAvailable()) {
            return blob;
        }

        BlobHolder result = service.convert(PICTURE_RESIZE_CONVERTER, bh,
                parameters);

        if (result != null) {
            return result.getBlob();
        } else {
            Blob fakeRes = new StringBlob("Converter did not return any result");
            fakeRes.setMimeType("text/plain");
            return fakeRes;
        }
    }

}
