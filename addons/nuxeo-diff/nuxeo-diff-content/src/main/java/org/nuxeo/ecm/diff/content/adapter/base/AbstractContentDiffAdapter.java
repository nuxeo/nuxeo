/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content.adapter.base;

import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.diff.content.ContentDiffAdapter;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.diff.content.ContentDiffHelper;
import org.nuxeo.ecm.diff.content.adapter.ContentDiffAdapterManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract base class for content diff adapters.
 *
 * @author Antoine Taillefer
 * @since 5.6
 */
public abstract class AbstractContentDiffAdapter implements ContentDiffAdapter {

    protected DocumentModel adaptedDoc;

    public String getFileContentDiffURL(DocumentModel otherDoc, ContentDiffConversionType conversionType, String locale) {
        return ContentDiffHelper.getContentDiffURL(adaptedDoc, otherDoc, conversionType.name(), locale);
    }

    public String getFileContentDiffURL(DocumentModel otherDoc, String xpath, ContentDiffConversionType conversionType,
            String locale) {
        return ContentDiffHelper.getContentDiffURL(adaptedDoc, otherDoc, xpath, conversionType.name(), locale);
    }

    public List<Blob> getFileContentDiffBlobs(DocumentModel otherDoc, ContentDiffConversionType conversionType,
            Locale locale) throws ContentDiffException, ConversionException {
        return getContentDiffBlobs(otherDoc, conversionType, locale);
    }

    public List<Blob> getFileContentDiffBlobs(DocumentModel otherDoc, String xpath,
            ContentDiffConversionType conversionType, Locale locale) throws ContentDiffException, ConversionException {
        return getContentDiffBlobs(otherDoc, xpath, conversionType, locale);
    }

    protected abstract List<Blob> getContentDiffBlobs(DocumentModel otherDoc, ContentDiffConversionType conversionType,
            Locale locale) throws ContentDiffException, ConversionException;

    protected abstract List<Blob> getContentDiffBlobs(DocumentModel otherDoc, String xpath,
            ContentDiffConversionType conversionType, Locale locale) throws ContentDiffException, ConversionException;

    public void setAdaptedDocument(DocumentModel doc) {
        this.adaptedDoc = doc;
    }

    /**
     * Gets the content diff adapter manager.
     *
     * @return the content diff adapter manager
     */
    protected final ContentDiffAdapterManager getContentDiffAdapterManager() {
        return Framework.getService(ContentDiffAdapterManager.class);
    }

}
