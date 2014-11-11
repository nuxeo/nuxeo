package org.nuxeo.ecm.platform.convert.plugins;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;

public class WordPerfect2TextConverter extends CommandLineBasedConverter {

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput,
            CmdParameters cmdParams) throws ConversionException {

        StringBuffer sb = new StringBuffer();

        if (cmdOutput!=null) {
            for (String out : cmdOutput) {
                if (out!=null && out.trim().length()>0) {
                    sb.append(out.trim());
                    sb.append("\n");
                }
            }
        }
        return new SimpleCachableBlobHolder(new StringBlob(sb.toString()));
    }

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Map<String, Blob> cmdBlobParams = new HashMap<String, Blob>();
        try {
            cmdBlobParams.put("inFilePath", blobHolder.getBlob());
        } catch (ClientException e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        return new HashMap<String, String>();
    }


}
