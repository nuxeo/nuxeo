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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types.primitives;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.schema.types.PrimitiveType;

/**
 * The binary type handles values of type InpuStream.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class BinaryType extends PrimitiveType {

    public static final String ID = "binary";

    public static final BinaryType INSTANCE = new BinaryType();

    private static final long serialVersionUID = 3424217422110579879L;

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
        if (str == null) {
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
                e.printStackTrace();
                return null;
            }
        }
        return object.toString();
    }

}
