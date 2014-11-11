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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.InputStream;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.model.PropertyException;
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

    public Object create(Map<String, Object> value) throws PropertyException {
        String mimeType = (String) value.get("mime-type");
        String encoding = (String) value.get("encoding");
//        long length = -1;
//        Long lengthVal = (Long)value.get("length");
//        if (lengthVal != null) {
//            length = lengthVal.longValue();
//        }
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

        StreamingBlob blob = new StreamingBlob(ss);
        blob.setMimeType(mimeType);
        blob.setEncoding(encoding);
//        blob.setFilename(filename);
//        blob.setDigest(digest);
        return blob;
    }

}
