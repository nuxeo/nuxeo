package org.nuxeo.template.processors.convert;

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
            String destMimeType) throws Exception {
        ConversionService cs = Framework.getLocalService(ConversionService.class);

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("updateDocumentIndex", Boolean.TRUE);
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
            return applyConverter(blob, converter, mimeType);
        }
        return blob;
    }
}