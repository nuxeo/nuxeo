/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.binary.metadata.api.listener;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.ABOUT_TO_CREATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.binary.metadata.api.service.BinaryMetadataService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.1
 */
public class BinaryMetadataListener implements EventListener {

    protected final BinaryMetadataService binaryMetadataService;

    public BinaryMetadataListener() {
        this.binaryMetadataService = Framework.getLocalService(BinaryMetadataService.class);
    }

    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (ABOUT_TO_CREATE.equals(event.getName()) && !doc.isProxy()) {
            BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
            if (blobHolder != null) {
                Blob blob = blobHolder.getBlob();
                if (blob != null) {
                    binaryMetadataService.writeMetadata(doc, docCtx.getCoreSession());

                }
            }
        } else if (BEFORE_DOC_UPDATE.equals(event.getName()) && !doc.isProxy()) {
            Property fileProp = doc.getProperty("file:content");
            Map<String, Object> dirtyMetadata = getDirtyMapping(doc);
            if (fileProp.isDirty()) {
                if (dirtyMetadata != null && !dirtyMetadata.isEmpty()) {
                    // if Blob dirty and document metadata dirty, write metadata from doc to Blob
                    binaryMetadataService.writeMetadata(fileProp.getValue(Blob.class), dirtyMetadata);
                } else {
                    // if Blob dirty and document metadata not dirty, write metadata from Blob to doc
                    binaryMetadataService.writeMetadata(doc, ctx.getCoreSession());
                }
            } else {
                if (dirtyMetadata != null && !dirtyMetadata.isEmpty()) {
                    // if Blob not dirty and document metadata dirty, write metadata from doc to Blob
                    binaryMetadataService.writeMetadata(fileProp.getValue(Blob.class), dirtyMetadata);
                }
            }
        }
    }

    private Map<String, Object> getDirtyMapping(DocumentModel doc) {
        Map<String, Object> resultDirtyMapping  =  new HashMap<>();
        Map<String, String> resultMapping =  this.binaryMetadataService.getMappingMetadata(doc);
        for(String metadata: resultMapping.keySet()){
            Property property = doc.getProperty(metadata);
            if(property.isDirty()){
                resultDirtyMapping.put(resultMapping.get(metadata),doc.getPropertyValue(metadata));
            }
        }
        return resultDirtyMapping;
    }
}
