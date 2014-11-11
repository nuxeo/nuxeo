/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema.types.primitives;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.types.PrimitiveType;

/**
 * The binary type handles values of type InputStream.
 */
public final class BinaryType extends PrimitiveType {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(BinaryType.class);

    public static final String ID = "binary";

    public static final BinaryType INSTANCE = new BinaryType();

    private BinaryType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        return true;
    }

    @Override
    public Object convert(Object value) {
        if (value instanceof CharSequence) {
            return new ByteArrayInputStream(value.toString().getBytes());
        } else if (value instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) value);
        } else if (value instanceof InputStream) {
            return value;
        }
        return null;
    }

    public static Object parseString(String str) {
        return new ByteArrayInputStream(str.getBytes());
    }

    protected Object readResolve() {
        return INSTANCE;
    }

    @Override
    public Object decode(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        return new ByteArrayInputStream(str.getBytes());
    }

    @Override
    public String encode(Object object) {
        if (object instanceof InputStream) {
            try {
                return FileUtils.read((InputStream) object);
            } catch (Exception e) {
                log.error(e, e);
                return null;
            }
        }
        return object.toString();
    }

}
