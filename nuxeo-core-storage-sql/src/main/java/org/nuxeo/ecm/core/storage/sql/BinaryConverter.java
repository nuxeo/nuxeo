/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to convert binaries.
 *
 * @author Florent Guillaume
 */
public class BinaryConverter {

    private static final Log log = LogFactory.getLog(BinaryConverter.class);

    public static final String ANY2TEXT = "any2text";

    protected boolean initialized;

    protected ConversionService conversionService;

    /*
     * Not done in a constructor as it would be called too early before all
     * services are registered.
     */
    private boolean initialized() {
        if (!initialized) {
            initialized = true;
            try {
                conversionService = Framework.getService(ConversionService.class);
            } catch (Exception e) {
                log.error("Cannot get ConversionService", e);
            }
        }
        return conversionService != null;
    }

    /**
     * Converts a binary to text.
     *
     * @return a string or {@code null} if conversion failed
     */
    public String getString(Binary binary, String mimeType) {
        if (!initialized()) {
            return null;
        }
        try {
            Blob blob = new InputStreamBlob(binary.getStream(), mimeType);
            SimpleBlobHolder bh = new SimpleBlobHolder(blob);
            BlobHolder result = conversionService.convert(ANY2TEXT, bh, null);
            if (result == null) {
                return null;
            }
            blob = result.getBlob();
            if (blob == null) {
                return null;
            }
            return new String(blob.getByteArray(), "UTF-8");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
