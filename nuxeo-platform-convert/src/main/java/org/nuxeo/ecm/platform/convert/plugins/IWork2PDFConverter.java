/*
 * (C) Copyright 2002-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * iWork2PDF converter.
 *
 * @author ldoguin
 */
public class IWork2PDFConverter implements Converter {

    public static final List<String> IWORK_MIME_TYPES = Arrays.asList(new String[] {
            "application/vnd.apple.pages", "application/vnd.apple.keynote",
            "application/vnd.apple.numbers", "application/vnd.apple.iwork" });

    private static final String IWORK_PREVIEW_FILE = "QuickLook/Preview.pdf";

    @Override
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        try {
            // retrieve the blob and verify its mimeType
            Blob blob = blobHolder.getBlob();
            String mimeType = blob.getMimeType();

            if (mimeType == null
                    || !IWORK_MIME_TYPES.contains(mimeType)) {
                throw new ConversionException("not an iWork file");
            }
            // look for the pdf file
            if (ZipUtils.hasEntry(blob.getStream(), IWORK_PREVIEW_FILE)) {
                // pdf file exist, let's extract it and return it as a
                // BlobHolder.
                InputStream previewPDFFile = ZipUtils.getEntryContentAsStream(
                        blob.getStream(), IWORK_PREVIEW_FILE);
                Blob previewBlob = new FileBlob(previewPDFFile);
                return new SimpleCachableBlobHolder(previewBlob);
            } else {
                // Pdf file does not exist, conversion cannot be done.
                throw new ConversionException(
                        "iWork file does not contain a pdf preview.");
            }
        } catch (Exception e) {
            throw new ConversionException(
                    "Could not find the pdf preview in the iWork file", e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
