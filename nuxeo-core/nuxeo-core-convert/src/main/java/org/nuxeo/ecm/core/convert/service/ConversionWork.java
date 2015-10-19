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

package org.nuxeo.ecm.core.convert.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
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

    public ConversionWork(String converterName, String destinationMimeType, BlobHolder blobHolder,
            Map<String, Serializable> parameters) {
        super();
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
        inputEntryKey = entryKey + "_input";
        putBlobHolder(inputEntryKey, blobHolder);
    }

    @Override
    public void work() {
        setStatus("Converting");

        BlobHolder inputBlobHolder = retrieveInputBlobHolder();
        if (inputBlobHolder == null) {
            return;
        }

        ConversionService conversionService = Framework.getService(ConversionService.class);
        BlobHolder result = converterName != null ? conversionService.convert(converterName, inputBlobHolder,
                parameters) : conversionService.convertToMimeType(destinationMimeType, inputBlobHolder, parameters);
        if (result == null) {
            return;
        }

        putBlobHolder(result);

        setStatus(null);
    }

    protected BlobHolder retrieveInputBlobHolder() {
        return getBlobHolder(inputEntryKey);
    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        super.cleanUp(ok, e);
        removeBlobHolder(inputEntryKey);
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
