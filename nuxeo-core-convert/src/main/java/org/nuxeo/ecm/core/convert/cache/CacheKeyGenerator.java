package org.nuxeo.ecm.core.convert.cache;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

public class CacheKeyGenerator {


    public static String computeKey(String converterName, BlobHolder blobHolder,
            Map<String, Serializable> parameters) {

        StringBuffer sb = new StringBuffer();

        sb.append(converterName);
        sb.append(":");
        try {
            sb.append(blobHolder.getHash());
        } catch (ClientException e) {
            throw new IllegalStateException("Can not fetch Hash from BlobHolder");
        }

        if (parameters!=null) {
            for (String key : parameters.keySet()) {
                sb.append(":");
                sb.append(key);
                sb.append(":");
                sb.append(parameters.get(key).toString());
            }
        }
        return sb.toString();
    }

}
