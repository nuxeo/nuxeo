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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.transientstore.StorageEntryImpl;
import org.nuxeo.ecm.core.transientstore.api.StorageEntry;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Work executing a given conversion.
 *
 * @since 7.4
 */
public class ConversionWork extends TransientStoreWork {

    protected String converterName;

    protected Map<String, Serializable> parameters;

    protected String inputEntryKey;

    public ConversionWork(String converterName, BlobHolder blobHolder, Map<String, Serializable> parameters) {
        super();
        this.converterName = converterName;
        this.parameters = parameters;
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }

        storeInputBlobHolder(blobHolder);
    }

    protected void storeInputBlobHolder(BlobHolder blobHolder) {
        inputEntryKey = entryKey + "_input";
        StorageEntry entry = new StorageEntryImpl(inputEntryKey);
        entry.setBlobs(blobHolder.getBlobs());
        Map<String, Serializable> properties = blobHolder.getProperties();
        if (properties != null) {
            entry.putAll(properties);
        }
        saveStorageEntry(entry);
    }

    @Override
    public void work() {
        setStatus("Converting");

        BlobHolder inputBlobHolder = retrieveInputBlobHolder();
        if (inputBlobHolder == null) {
            return;
        }

        ConversionService conversionService = Framework.getService(ConversionService.class);
        BlobHolder result = conversionService.convert(converterName, inputBlobHolder, parameters);
        if (result == null) {
            return;
        }

        StorageEntry entry = getStorageEntry();
        entry.setBlobs(result.getBlobs());
        Map<String, Serializable> properties = result.getProperties();
        if (properties != null) {
            entry.putAll(properties);
        }
        saveStorageEntry();

        setStatus(null);
    }

    protected BlobHolder retrieveInputBlobHolder() {
        StorageEntry inputEntry = getStorageEntry(inputEntryKey);
        if (inputEntry == null) {
            return null;
        }
        return new SimpleBlobHolderWithProperties(inputEntry.getBlobs(), inputEntry.getParameters());
    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        super.cleanUp(ok, e);
        getStore().remove(inputEntryKey);
    }

    @Override
    public String getTitle() {
        return String.format("Conversion using '%s' converter", converterName);
    }
}
