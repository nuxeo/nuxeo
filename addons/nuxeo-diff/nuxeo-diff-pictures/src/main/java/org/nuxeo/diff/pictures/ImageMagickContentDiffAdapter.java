/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
