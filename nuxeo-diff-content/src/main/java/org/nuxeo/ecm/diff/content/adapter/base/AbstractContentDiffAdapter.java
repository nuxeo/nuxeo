/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content.adapter.base;

import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
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

    public String getFileContentDiffURL(DocumentModel otherDoc,
            ContentDiffConversionType conversionType) {
        return ContentDiffHelper.getContentDiffURL(adaptedDoc, otherDoc,
                conversionType);
    }

    public String getFileContentDiffURL(DocumentModel otherDoc, String xpath,
            ContentDiffConversionType conversionType) {
        return ContentDiffHelper.getContentDiffURL(adaptedDoc, otherDoc, xpath,
                conversionType);
    }

    public List<Blob> getFileContentDiffBlobs(DocumentModel otherDoc,
            ContentDiffConversionType conversionType, Locale locale)
            throws ContentDiffException {
        return getContentDiffBlobs(otherDoc, conversionType, locale);
    }

    public List<Blob> getFileContentDiffBlobs(DocumentModel otherDoc,
            String xpath, ContentDiffConversionType conversionType,
            Locale locale) throws ContentDiffException {
        return getContentDiffBlobs(otherDoc, xpath, conversionType, locale);
    }

    protected abstract List<Blob> getContentDiffBlobs(DocumentModel otherDoc,
            ContentDiffConversionType conversionType, Locale locale)
            throws ContentDiffException;

    protected abstract List<Blob> getContentDiffBlobs(DocumentModel otherDoc,
            String xpath, ContentDiffConversionType conversionType,
            Locale locale) throws ContentDiffException;

    public void setAdaptedDocument(DocumentModel doc) {
        this.adaptedDoc = doc;
    }

    /**
     * Gets the content diff adapter manager.
     *
     * @return the content diff adapter manager
     * @throws ContentDiffException the content diff exception
     */
    protected final ContentDiffAdapterManager getContentDiffAdapterManager()
            throws ContentDiffException {

        ContentDiffAdapterManager contentDiffAdapterManager;
        try {
            contentDiffAdapterManager = Framework.getService(ContentDiffAdapterManager.class);
        } catch (Exception e) {
            throw new ContentDiffException(e);
        }
        if (contentDiffAdapterManager == null) {
            throw new ContentDiffException(
                    "ContentDiffAdapterManager service is null.");
        }
        return contentDiffAdapterManager;
    }

}
