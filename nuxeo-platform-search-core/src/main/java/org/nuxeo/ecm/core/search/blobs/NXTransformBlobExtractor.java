/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     anguenot
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.blobs;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.search.api.client.indexing.blobs.BlobExtractor;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.FulltextFieldDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Blob extractor that leverages Nuxeo transform service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class NXTransformBlobExtractor implements BlobExtractor {

    private static final long serialVersionUID = -4204325669629533663L;

    private static final int BYTE_ORDER_MARK_CHAR = 0xFEFF;

    private static ConversionService conversionService;

    private static ConversionService getConversionService() throws Exception {
        if (conversionService == null) {
            conversionService = Framework.getService(ConversionService.class);
        }
        return conversionService;
    }

    public String extract(Blob blob, String mimetype,
            FulltextFieldDescriptor desc) throws Exception {

        if (blob == null || blob.getLength() == 0) {
            return "";
        }

        String converterName = null;
        BlobHolder result;
        try {
            converterName = getConversionService().getConverterName(mimetype,
                    "text/plain");
            result = getConversionService().convert(converterName,
                    new SimpleBlobHolder(blob), null);
            return readContent(result.getBlob().getReader());
        } catch (IOException e) {
            throw new ClientException("Couldn't read from blob convert with "
                    + converterName, e);
        }
    }

    public static String readContent(Reader reader) throws IOException {
        char[] buffer = new char[2048];
        StringWriter writer = new StringWriter();
        int length;
        while ((length = reader.read(buffer, 0, 2048)) != -1) {
            writer.write(buffer, 0, length);
        }
        String content = stripByteOrderMarkChar(writer.toString());
        return content.trim();
    }

    private static String stripByteOrderMarkChar(String content) {
        if (content.length() > 0 && content.charAt(0) == BYTE_ORDER_MARK_CHAR) {
            return content.substring(1).trim();
        }
        return content;
    }

}
