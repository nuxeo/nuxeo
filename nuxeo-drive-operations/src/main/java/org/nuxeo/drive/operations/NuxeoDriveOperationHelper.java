/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;

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

}
