/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.utils.BlobsExtractor;

/**
 * {@link BlobHolder} implementation based on a {@link DocumentModel} and a XPath.
 *
 * @author tiry
 */
public class DocumentBlobHolder extends AbstractBlobHolder {

    protected final DocumentModel doc;

    protected final String xPath;

    protected List<Blob> blobList = null;

    public DocumentBlobHolder(DocumentModel doc, String xPath) {
        if (xPath == null) {
            throw new IllegalArgumentException("Invalid null xpath for document: " + doc);
        }
        this.doc = doc;
        this.xPath = xPath;
    }

    protected DocumentBlobHolder(DocumentModel doc, String xPath, List<Blob> blobList) {
        this(doc, xPath);
        this.blobList = blobList;
    }

    @Override
    protected String getBasePath() {
        return doc.getPathAsString();
    }

    @Override
    public Blob getBlob() {
        return (Blob) doc.getPropertyValue(xPath);
    }

    @Override
    public void setBlob(Blob blob) {
        doc.getProperty(xPath).setValue(blob);
    }

    @Override
    public Calendar getModificationDate() {
        return (Calendar) doc.getProperty("dublincore", "modified");
    }

    @Override
    public String getHash() {
        Blob blob = getBlob();
        if (blob != null) {
            String h = blob.getDigest();
            if (h != null) {
                return h;
            }
        }
        return doc.getId() + xPath + String.valueOf(getModificationDate());
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
    public List<Blob> getBlobs() {
        if (blobList == null) {
            computeBlobList();
        }
        return blobList;
    }

    /**
     * Returns a new {@link DocumentBlobHolder} for the blob at the given {@code index} where {@link #getBlob} and
     * {@link #getXpath} will return information about the blob.
     *
     * @param index the blob index
     * @return the new blob holder
     * @throws IndexOutOfBoundsException if the index is invalid
     * @since 9.3
     */
    public DocumentBlobHolder asDirectBlobHolder(int index) throws IndexOutOfBoundsException {
        List<Property> properties = computeBlobList();
        // find real xpath for the property at that index in the list
        String xpath = getFullXPath(properties.get(index));
        // keep the same blobList, even though its first element doesn't correspond to the xpath anymore
        return new DocumentBlobHolder(doc, xpath, blobList);
    }

    /**
     * Computes the blob list, with the main blob first.
     *
     * @return the blob properties
     * @since 9.3
     */
    protected List<Property> computeBlobList() {
        List<Property> properties = new BlobsExtractor().getBlobsProperties(doc);
        // be sure that the "main" blob is always in first position
        Iterator<Property> it = properties.iterator();
        boolean hasMainBlob = false;
        while (it.hasNext()) {
            Property property = it.next();
            if (getFullXPath(property).equals(xPath)) {
                it.remove();
                properties.add(0, property);
                hasMainBlob = true;
                break;
            }
        }
        if (!hasMainBlob) {
            // the main blob may not be coming from a blob property in subclasses, find its property anyway
            Property property = doc.getProperty(xPath);
            properties.add(0, property);
        }
        blobList = new ArrayList<>(properties.size());
        for (int i = 0; i < properties.size(); i++) {
            if (i == 0) {
                // the main blob may be computed differently in subclasses, always call getBlob()
                Blob mainBlob = getBlob();
                if (mainBlob != null) {
                    blobList.add(mainBlob);
                }
            } else {
                blobList.add((Blob) properties.get(i).getValue());
            }
        }
        return properties;
    }

    /**
     * Gets the full xpath for a property, including schema prefix in all cases.
     *
     * @since 9.3
     */
    protected String getFullXPath(Property property) {
        String xpath = property.getXPath();
        if (xpath.indexOf(':') < 0) {
            // add schema name as prefix
            xpath = property.getSchema().getName() + ':' + xpath;
        }
        return xpath;
    }

    /**
     * @since 7.3
     */
    public String getXpath() {
        return xPath;
    }

    /**
     * @since 7.4
     */
    public DocumentModel getDocument() {
        return doc;
    }
}
