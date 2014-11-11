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

import org.nuxeo.ecm.core.schema.types.PrimitiveType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class BooleanType extends PrimitiveType {

    public static final String ID = "boolean";

    public static final BooleanType INSTANCE = new BooleanType();

    private static final long serialVersionUID = 6734833141342781661L;

    private BooleanType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        return object instanceof Boolean;
    }

    public Object convert(Object value) {
        if (value instanceof Boolean) {
            return value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0 ? Boolean.TRUE
                    : Boolean.FALSE;
        } else {
            return Boolean.parseBoolean((String) value);
        }
    }

    @Override
    public Object decode(String str) {
        if (str == null) {
            return null;
        }
        return Boolean.parseBoolean(str);
    }

    @Override
    public String encode(Object value) {

        if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0 ? Boolean.TRUE.toString()
                    : Boolean.FALSE.toString();
        } else {
            return value != null ? (String) value : "";
        }
    }

    @Override
    public Object newInstance() {
        return false;
    }

    protected Object readResolve() {
        return INSTANCE;
    }

}
