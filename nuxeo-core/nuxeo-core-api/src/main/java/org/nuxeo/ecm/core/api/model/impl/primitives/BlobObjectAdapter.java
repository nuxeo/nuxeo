/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.InputStream;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.model.impl.osm.DynamicObjectAdapter;
import org.nuxeo.runtime.services.streaming.ByteArraySource;
import org.nuxeo.runtime.services.streaming.InputStreamSource;
import org.nuxeo.runtime.services.streaming.StreamSource;
import org.nuxeo.runtime.services.streaming.StringSource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class BlobObjectAdapter extends DynamicObjectAdapter {

    private static final long serialVersionUID = 4938888300516302200L;

    public BlobObjectAdapter() {
        super(Blob.class);
    }

    @Override
    public Object create(Map<String, Object> value) {
        String mimeType = (String) value.get("mime-type");
        String encoding = (String) value.get("encoding");
        // long length = -1;
        // Long lengthVal = (Long)value.get("length");
        // if (lengthVal != null) {
        // length = lengthVal.longValue();
        // }
        String filename = (String) value.get("name");
        String digest = (String) value.get("digest");
        Object data = value.get("data");
        StreamSource ss;
        if (data instanceof String) {
            ss = new StringSource((String) data);
        } else if (data instanceof InputStream) {
            ss = new InputStreamSource((InputStream) data);
        } else if (data instanceof byte[]) {
            ss = new ByteArraySource((byte[]) data);
        } else {
            ss = new ByteArraySource(new byte[0]);
        }

        Blob blob = new StreamingBlob(ss);
        blob.setMimeType(mimeType);
        blob.setEncoding(encoding);
        blob.setFilename(filename);
        blob.setDigest(digest);
        return blob;
    }

}
