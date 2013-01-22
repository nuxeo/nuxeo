/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.convert;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7
 */
public class ConvertHelper {

    protected String findConverter(Blob blob, String destMimeType)
            throws Exception {
        MimetypeRegistry mtr = Framework.getLocalService(MimetypeRegistry.class);
        String srcMt = mtr.getMimetypeFromFilenameAndBlobWithDefault(
                blob.getFilename(), blob, blob.getMimeType());
        blob.setMimeType(srcMt);
        ConversionService cs = Framework.getLocalService(ConversionService.class);
        return cs.getConverterName(srcMt, destMimeType);
    }

    protected Blob applyConverter(Blob blob, String converter,
            String destMimeType,  Map<String, Serializable> params) throws Exception {
        ConversionService cs = Framework.getLocalService(ConversionService.class);
        if (params == null ) {
            params = new HashMap<String, Serializable>();
            params.put("updateDocumentIndex", Boolean.TRUE);
        }
        BlobHolder bh = cs.convert(converter, new SimpleBlobHolder(blob),
                params);

        if (bh == null || bh.getBlob() == null) {
            return blob;
        } else {
            Blob result = bh.getBlob();
            MimetypeRegistry mtr = Framework.getLocalService(MimetypeRegistry.class);
            String filename = FileUtils.getFileNameNoExt(blob.getFilename());
            filename = filename + "."
                    + mtr.getExtensionsFromMimetypeName(destMimeType).get(0);
            result.setFilename(filename);
            return result;
        }
    }

    public Blob convertBlob(Blob blob, String mimeType) throws Exception {
        String converter = findConverter(blob, mimeType);
        if (converter != null) {
            return applyConverter(blob, converter, mimeType, null);
        }
        return blob;
    }

    public Blob convertBlob(Blob blob, String mimeType, Map<String, Serializable> params) throws Exception {
        String converter = findConverter(blob, mimeType);
        if (converter != null) {
            return applyConverter(blob, converter, mimeType, params);
        }
        return blob;
    }
}