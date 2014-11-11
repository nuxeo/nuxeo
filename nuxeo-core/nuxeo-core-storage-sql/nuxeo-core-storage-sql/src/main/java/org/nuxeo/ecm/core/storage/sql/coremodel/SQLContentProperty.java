/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.Node;

/**
 * A {@link SQLContentProperty} gives access to a blob, which consists of a
 * {@link Node} of a specialized type: {@code content}. One of the columns of
 * the row stores (indirectly) actual blob data using a {@code Binary}.
 *
 * @author Florent Guillaume
 */
public class SQLContentProperty extends SQLComplexProperty {

    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    // constants based on core-types.xsd fields. XXX Should be in model
    public static final String ENCODING = "encoding";

    public static final String MIME_TYPE = "mime-type";

    public static final String FILE_NAME = "name";

    public static final String DIGEST = "digest";

    public static final String LENGTH = "length";

    public static final String BINARY = "data";

    public SQLContentProperty(Node node, ComplexType type, SQLSession session,
            boolean readonly) {
        super(node, type, session, readonly);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getValue() throws DocumentException {
        Map<String, Object> map = (Map<String, Object>) super.getValue();
        Binary binary = (Binary) map.get(BINARY);
        if (binary == null) {
            return null;
        }
        String filename = (String) map.get(FILE_NAME);
        String mimeType = (String) map.get(MIME_TYPE);
        String encoding = (String) map.get(ENCODING);
        String digest = (String) map.get(DIGEST);
        Long length = (Long)map.get(LENGTH);
        return new SQLBlob(binary, filename, mimeType, encoding, digest, length.longValue());
    }

    @Override
    public void setValue(Object value) throws DocumentException {
        checkWritable();
        Map<String, Object> map;
        if (value == null) {
            // nothing, use null
            map = null;
        } else if (value instanceof Blob) {
            map = new HashMap<String, Object>();
            Blob blob = (Blob) value;
            Binary binary;
            binary = session.getBinary(blob);
            String filename = blob.getFilename();
            String mimeType = blob.getMimeType();
            if (mimeType == null) {
                mimeType = APPLICATION_OCTET_STREAM;
            }
            String encoding = blob.getEncoding();
            String digest = blob.getDigest();
            Long length = blob.getLength();
            // length computed from actual binary
            if (length == null || length == -1) {
                length = binary.getLength();
            }
            map.put(BINARY, binary);
            map.put(FILE_NAME, filename);
            map.put(MIME_TYPE, mimeType);
            map.put(ENCODING, encoding);
            map.put(DIGEST, digest);
            map.put(LENGTH, length);
        } else {
            throw new DocumentException("Setting a non-Blob value: " + value);
        }
        super.setValue(map);
    }

}
