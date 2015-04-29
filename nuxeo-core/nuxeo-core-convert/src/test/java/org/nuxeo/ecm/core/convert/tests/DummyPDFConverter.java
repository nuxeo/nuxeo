/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.convert.tests;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * @since 7.3
 */
public class DummyPDFConverter implements Converter {

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        Boolean setMimeType = (Boolean) parameters.get("setMimeType");
        if (setMimeType == null) {
            setMimeType = false;
        }
        Boolean tempFilename = (Boolean) parameters.get("tempFilename");
        if (tempFilename == null) {
            tempFilename = false;
        }
        Blob blob = Blobs.createBlob("", setMimeType ? "application/octet-stream" : null);
        if (tempFilename) {
            blob.setFilename("nxblob-434523.tmp");
        }
        return new SimpleBlobHolder(blob);
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
