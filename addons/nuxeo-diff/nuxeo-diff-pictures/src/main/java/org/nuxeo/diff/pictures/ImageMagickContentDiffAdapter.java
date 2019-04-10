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
 *     Thibaud Arguillere
 */
package org.nuxeo.diff.pictures;

import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.diff.content.ContentDiffException;
import org.nuxeo.ecm.diff.content.adapter.MimeTypeContentDiffer;
import org.nuxeo.ecm.diff.content.adapter.base.AbstractContentDiffAdapter;
import org.nuxeo.ecm.diff.content.adapter.base.ContentDiffConversionType;

/**
 * @since 7.4
 */
public class ImageMagickContentDiffAdapter extends AbstractContentDiffAdapter {

    public static final String IMAGE_MAGIC_CONTENT_DIFFER_NAME = "imageMagickContentDiffer";

    @Override
    public boolean cachable() {

        return true;
    }

    @Override
    public void cleanup() {
        // Cleanup your stuff here
    }

    @Override
    protected List<Blob> getContentDiffBlobs(DocumentModel otherDoc, ContentDiffConversionType conversionType,
            Locale locale) throws ContentDiffException, ConversionException {

        return getContentDiffBlobs(otherDoc, null, conversionType, locale);
    }

    @Override
    protected List<Blob> getContentDiffBlobs(DocumentModel otherDoc, String xpath,
            ContentDiffConversionType conversionType, Locale locale) throws ContentDiffException, ConversionException {

        MimeTypeContentDiffer contentDiffer = getContentDiffAdapterManager().getContentDifferForName(
                IMAGE_MAGIC_CONTENT_DIFFER_NAME);
        if (contentDiffer instanceof ImageMagickContentDiffer) {
            ImageMagickContentDiffer imContentDiffer = (ImageMagickContentDiffer) contentDiffer;
            return imContentDiffer.getContentDiff(adaptedDoc, otherDoc, xpath, locale);
        }
        throw new ContentDiffException("The contentDiffer for '" + IMAGE_MAGIC_CONTENT_DIFFER_NAME
                + "' should be a ImageMagickContentDiffer. Check the xml contribution.");
    }

}
