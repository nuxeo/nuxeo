/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.impl.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.schema.types.TypeException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class BlobConverter extends ValueConverter {

    public static final BlobConverter INSTANCE = new BlobConverter();

    private BlobConverter() { }

    @Override
    public Object convert(Object value) throws TypeException {
        if (value instanceof Map) {
            Map map = (Map) value;
            String encoding = (String) map.get("encoding");
            String mimeType = (String) map.get("mime-type");
            Object data = map.get("data");
            Blob blob;
            if (data.getClass() == byte[].class) {
                blob = new ByteArrayBlob((byte[]) data, mimeType, encoding);
            } else if (data instanceof InputStream) {
                try {
                    blob = new FileBlob((InputStream) data, mimeType, encoding);
                } catch (IOException e) {
                    throw new TypeException("Failed to convert to blob "
                            + value.getClass(), e);
                }
            } else {
                String str = data.toString();
                blob = new StringBlob(str, mimeType, encoding);
            }
            // TODO handle now the encoding?
            return blob;
        } else if (value instanceof Blob) {
            return value;
        }
        throw new TypeException("Cannot convert value " + value.getClass()
                + " to type 'content'");
    }

}
