/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.platform.rendition.adapter;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.AbstractBlobHolder;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Blob holder able to return the blob of a document default rendition.
 *
 * @see RenditionService#getDefaultRendition(DocumentModel, String, Map)
 * @since 9.3
 */
public class DownloadBlobHolder extends AbstractBlobHolder {

    protected final DocumentModel doc;

    public DownloadBlobHolder(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public Serializable getProperty(String name) {
        return null;
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return null;
    }

    @Override
    public Blob getBlob() {
        Rendition rendition = Framework.getService(RenditionService.class).getDefaultRendition(doc, "download",
                null);
        return rendition != null ? rendition.getBlob() : null;
    }

    @Override
    protected String getBasePath() {
        return doc.getPathAsString();
    }

    @Override
    public Calendar getModificationDate() {
        return (Calendar) doc.getPropertyValue("dc:modified");
    }

}
