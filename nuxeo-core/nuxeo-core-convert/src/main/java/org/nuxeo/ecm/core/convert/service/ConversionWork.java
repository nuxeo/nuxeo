/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.convert.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Work executing a given conversion.
 *
 * @since 7.4
 */
public class ConversionWork extends TransientStoreWork {

    private static final long serialVersionUID = 14593653977944460L;

    protected String converterName;

    protected String destinationMimeType;

    protected Map<String, Serializable> parameters;

    protected String inputEntryKey;

    /**
     * @since 10.1
     */
    protected ManagedBlob managedBlob;

    public ConversionWork(String converterName, String destinationMimeType, BlobHolder blobHolder,
            Map<String, Serializable> parameters) {
        if (converterName == null && destinationMimeType == null) {
            throw new IllegalArgumentException("'convertName' or 'destinationMimeType' must not be null");
        }
        this.converterName = converterName;
        this.destinationMimeType = destinationMimeType;
        this.parameters = parameters;
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }

        storeInputBlobHolder(blobHolder);
    }

    protected void storeInputBlobHolder(BlobHolder blobHolder) {
        if (!storeManagedBlob(blobHolder)) {
            // standard conversion
            inputEntryKey = entryKey + "_input";
            putBlobHolder(inputEntryKey, blobHolder);
        }
    }

    /**
     * @since 10.1
     */
    protected boolean storeManagedBlob(BlobHolder blobHolder) {
        Blob blob = blobHolder.getBlob();
        if (!(blob instanceof ManagedBlob) || !(blob instanceof Serializable) || blobHolder.getBlobs().size() > 1) {
            return false;
        }

        ManagedBlob mBlob = (ManagedBlob) blob;
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(mBlob);
        if (blobProvider.canConvert(mBlob, destinationMimeType)) {
            this.managedBlob = mBlob;
            return true;
        }
        return false;
    }

    @Override
    public void work() {
        setStatus("Converting");

        BlobHolder inputBlobHolder = retrieveInputBlobHolder();
        if (inputBlobHolder == null) {
            return;
        }

        ConversionService conversionService = Framework.getService(ConversionService.class);
        BlobHolder result = converterName != null
                ? conversionService.convert(converterName, inputBlobHolder, parameters)
                : conversionService.convertToMimeType(destinationMimeType, inputBlobHolder, parameters);
        if (result == null) {
            return;
        }

        putBlobHolder(result);

        setStatus(null);
    }

    protected BlobHolder retrieveInputBlobHolder() {
        return managedBlob != null ? new SimpleBlobHolder(managedBlob) : getBlobHolder(inputEntryKey);
    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        super.cleanUp(ok, e);
        if (inputEntryKey != null) {
            removeBlobHolder(inputEntryKey);
        }
    }

    @Override
    public String getTitle() {
        if (converterName != null) {
            return String.format("Conversion using '%s' converter", converterName);
        } else {
            return String.format("Conversion using '%s' target mime type", destinationMimeType);
        }
    }
}
