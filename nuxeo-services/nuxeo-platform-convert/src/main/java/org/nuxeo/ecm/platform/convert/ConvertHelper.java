/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.convert;

import java.io.Serializable;
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

    protected String findConverter(Blob blob, String destMimeType) {
        MimetypeRegistry mtr = Framework.getService(MimetypeRegistry.class);
        String srcMt = mtr.getMimetypeFromFilenameAndBlobWithDefault(blob.getFilename(), blob, blob.getMimeType());
        blob.setMimeType(srcMt);
        ConversionService cs = Framework.getService(ConversionService.class);
        return cs.getConverterName(srcMt, destMimeType);
    }

    protected Blob applyConverter(Blob blob, String converter, String destMimeType, Map<String, Serializable> params) {
        ConversionService cs = Framework.getService(ConversionService.class);
        BlobHolder bh = cs.convert(converter, new SimpleBlobHolder(blob), params);

        if (bh == null || bh.getBlob() == null) {
            return blob;
        } else {
            Blob result = bh.getBlob();
            MimetypeRegistry mtr = Framework.getService(MimetypeRegistry.class);
            String filename = FileUtils.getFileNameNoExt(blob.getFilename());
            filename = filename + "." + mtr.getExtensionsFromMimetypeName(destMimeType).get(0);
            result.setFilename(filename);
            if (result.getMimeType() == null) {
                result.setMimeType(destMimeType);
            }
            return result;
        }
    }

    public Blob convertBlob(Blob blob, String mimeType) {
        String converter = findConverter(blob, mimeType);
        if (converter != null) {
            return applyConverter(blob, converter, mimeType, null);
        }
        return blob;
    }

    public Blob convertBlob(Blob blob, String mimeType, Map<String, Serializable> params) {
        String converter = findConverter(blob, mimeType);
        if (converter != null) {
            return applyConverter(blob, converter, mimeType, params);
        }
        return blob;
    }
}
