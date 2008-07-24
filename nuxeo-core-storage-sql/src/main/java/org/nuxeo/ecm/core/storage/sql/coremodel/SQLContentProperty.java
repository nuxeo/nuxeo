/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.impl.blob.LazyBlob;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * A {@link SQLContentProperty} gives access to a blob, which consists of a
 * {@link Node} of a specialized type: {@code content}. One of the columns of
 * the row stores (indirectly) actual blob data.
 *
 * @author Florent Guillaume
 */
public class SQLContentProperty extends SQLComplexProperty {

    // constants based on core-types.xsd fields. XXX Should be in model
    public static final String ENCODING = "encoding";

    public static final String MIME_TYPE = "mime-type";

    public static final String FILE_NAME = "name";

    public static final String DIGEST = "digest";

    public static final String LENGTH = "length";

    public SQLContentProperty(Node node, ComplexType type, SQLSession session) {
        super(node, type, session);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getValue() throws DocumentException {
        Map<String, Object> map = (Map<String, Object>) super.getValue();
        String sid = null; // getSession().getUserSessionId();
        String dataKey = "/XXX/foo/bar/DATA"; // Used for what ? XXX
        String repositoryName = session.getRepository().getName();
        InputStream stream = null; // JCRBlobInputStream
        // XXX TODO stream
        String encoding = (String) map.get(ENCODING);
        String mimeType = (String) map.get(MIME_TYPE);
        String filename = (String) map.get(FILE_NAME);
        String digest = (String) map.get(DIGEST);
        Long llength = (Long) map.get(LENGTH);
        long length = llength == null ? 0 : llength.longValue();
        return new LazyBlob(stream, encoding, mimeType, sid, dataKey,
                repositoryName, filename, digest, length);
    }

    @Override
    public void setValue(Object value) throws DocumentException {
        // XXX AT: set null value insted of HashMap, waiting for NXP-912
        Map<String, Object> map = new HashMap<String, Object>();
        if (value instanceof Blob) {
            Blob blob = (Blob) value;
            String encoding = blob.getEncoding();
            String mimeType = blob.getMimeType();
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            String filename = blob.getFilename();
            String digest = blob.getDigest();
            Long length = Long.valueOf(blob.getLength());
            map.put(ENCODING, encoding);
            map.put(MIME_TYPE, mimeType);
            map.put(FILE_NAME, filename);
            map.put(DIGEST, digest);
            map.put(LENGTH, length);
            // XXX TODO stream
        }
        super.setValue(map);
    }

}
