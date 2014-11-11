/*
 * (C) Copyright 2002-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * Zip2Html converter.
 *
 * @author ldoguin
 */
public class Zip2HtmlConverter implements Converter {

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        try {
            Blob blob = blobHolder.getBlob();
            String mimeType = blob.getMimeType();
            if (!mimeType.equals("application/zip")) {
                throw new ConversionException("not a zip file");
            }
            return new ZipCachableBlobHolder(blob);
        } catch (Exception e) {
            throw new ConversionException("Could not get Zip File from BlobHolder", e);
        }
    }

    public void init(ConverterDescriptor descriptor) {
    }

}
