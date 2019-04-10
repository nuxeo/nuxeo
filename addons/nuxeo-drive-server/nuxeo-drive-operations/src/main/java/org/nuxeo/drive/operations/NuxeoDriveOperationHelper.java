/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import java.io.IOException;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;

/**
 * Helper for Nuxeo Drive operations.
 * 
 * @author Antoine Taillefer
 */
public final class NuxeoDriveOperationHelper {

    private NuxeoDriveOperationHelper() {
        // Helper class
    }

    public static void normalizeMimeTypeAndEncoding(Blob blob) throws ParseException {

        String mimeType = blob.getMimeType();
        if (!StringUtils.isEmpty(mimeType) && !"null".equals(mimeType)) {
            ContentType contentType = new ContentType(mimeType);
            blob.setMimeType(contentType.getBaseType());
            if (StringUtils.isEmpty(blob.getEncoding())) {
                String charset = contentType.getParameter("charset");
                if (!StringUtils.isEmpty(charset)) {
                    blob.setEncoding(charset);
                }
            }
        }
    }

    public static Blob asJSONBlob(Object value) throws JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(value);
        return StreamingBlob.createFromByteArray(json.getBytes("UTF-8"), "application/json");
    }

}
