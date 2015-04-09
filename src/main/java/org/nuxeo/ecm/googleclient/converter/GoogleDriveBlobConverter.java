/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Nelson Silva
 */
package org.nuxeo.ecm.googleclient.converter;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * Converter that relies on {@link org.nuxeo.ecm.core.blob.BlobProvider} conversions.
 *
 * @since 7.3
 */
public class GoogleDriveBlobConverter implements Converter {

    protected ConverterDescriptor descriptor;

    @Override
    public void init(ConverterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        Blob srcBlob, dstBlob;

        try {
            srcBlob = blobHolder.getBlob();
        } catch (ClientException e) {
            throw new ConversionException("Unable to fetch Blob", e);
        }

        if ((srcBlob == null) || !(srcBlob instanceof ManagedBlob)) {
            return null;
        }

        try {
            dstBlob = convert((ManagedBlob) srcBlob);
        } catch (IOException e) {
            throw new ConversionException("Unable to fetch conversion", e);
        }

        if (dstBlob == null) {
            return null;
        }

        return new SimpleCachableBlobHolder(dstBlob);
    }

    protected Blob convert(ManagedBlob blob) throws IOException {
        String mimetype = descriptor.getDestinationMimeType();
        InputStream is = blob.getConvertedStream(mimetype);
        return (is == null) ? null : Blobs.createBlob(is, mimetype);
    }
}
