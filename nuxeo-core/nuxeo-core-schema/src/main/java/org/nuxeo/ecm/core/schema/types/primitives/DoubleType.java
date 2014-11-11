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
 */
public final class DoubleType extends PrimitiveType {

    public static final String ID = "double";

    public static final DoubleType INSTANCE = new DoubleType();

    private static final long serialVersionUID = -1663322345944576176L;

    private DoubleType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        return object instanceof Number;
    }

    public Object convert(Object value) {
        if (value instanceof Double) {
            return value;
        } else if (value instanceof Number) {
            return (double) ((Number) value).intValue();
        } else {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @Override
    public Object decode(String str) {
        if (str == null) {
            return null;
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public String encode(Object object) {
        if (object instanceof Double) {
            return object.toString();
        } else if (object instanceof Number) {
            return object.toString();
        } else {
            return object != null ? (String) object : "";
        }
    }

    protected Object readResolve() {
        return INSTANCE;
    }

}
