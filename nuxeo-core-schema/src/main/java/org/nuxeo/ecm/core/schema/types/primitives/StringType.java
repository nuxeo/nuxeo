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
public final class StringType extends PrimitiveType {

    public static final String ID = "string";

    public static final StringType INSTANCE = new StringType();

    private static final long serialVersionUID = -6451420665839530152L;

    private StringType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        return true;
    }

    public Object convert(Object value) {
        return value.toString();
    }

    @Override
    public Object decode(String str) {
        return str;
    }

    @Override
    public String encode(Object object) {
        return object != null ? object.toString() : "";
    }

    @Override
    public Object newInstance() {
        return "";
    }

    protected Object readResolve() {
        return INSTANCE;
    }

}
