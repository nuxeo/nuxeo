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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.core.convert.api;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.blob.ManagedBlob;

/**
 * Base exception raised by the {@link ConversionService}.
 *
 * @author tiry
 */
public class ConversionException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public ConversionException() {
        super();
    }

    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversionException(Throwable cause) {
        super(cause);
    }

    /**
     * @since 11.1
     */
    public ConversionException(String message, BlobHolder blobHolder) {
        super(message);
        addInfos(blobHolder);
    }

    /**
     * @since 11.1
     */
    public ConversionException(String message, BlobHolder blobHolder, Throwable cause) {
        super(message, cause);
        addInfos(blobHolder);
    }

    /**
     * @since 11.1
     */
    public ConversionException(BlobHolder blobHolder, Throwable cause) {
        super(cause);
        addInfos(blobHolder);
    }

    /**
     * @since 11.1
     */
    public ConversionException(String message, Blob blob, Throwable cause) {
        super(message, cause);
        addInfo(blob);
    }

    /**
     * @since 11.1
     */
    public ConversionException(String message, Blob blob) {
        super(message);
        addInfo(blob);
    }

    protected void addInfos(BlobHolder blobHolder) {
        if (blobHolder instanceof DocumentBlobHolder) {
            DocumentBlobHolder documentBlobHolder = (DocumentBlobHolder) blobHolder;
            addInfo(String.format("Document: %s", documentBlobHolder.getDocument()));
        }

        List<Blob> blobs = blobHolder.getBlobs();
        if (blobs != null) {
            blobs.forEach(this::addInfo);
        }
    }

    protected void addInfo(Blob blob) {
        if (blob instanceof ManagedBlob) {
            ManagedBlob managedBlob = (ManagedBlob) blob;
            addInfo(String.format("Blob Key/Provider: %s/%s", managedBlob.getKey(), managedBlob.getProviderId()));
        }
    }
}
