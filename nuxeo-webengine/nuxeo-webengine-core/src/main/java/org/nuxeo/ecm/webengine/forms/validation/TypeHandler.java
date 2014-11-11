/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.forms.validation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class TypeHandler {

    protected static final Map<String, TypeHandler> handlers = new HashMap<String, TypeHandler>();

    protected final String type;

    protected TypeHandler(String type) {
        this.type = type;
        handlers.put(type, this);
    }

    public static TypeHandler getHandler(String type) {
        return handlers.get(type);
    }

    public String getType() {
        return type;
    }

    public abstract Object decode(String value) throws TypeException;

    public static final TypeHandler STRING = new TypeHandler("string") {
        @Override
        public Object decode(String value) throws TypeException {
            return value;
        }
    };

    public static final TypeHandler INTEGER = new TypeHandler("integer") {
        @Override
        public Object decode(String value) { return Integer.valueOf(value); }
    };

    public static final TypeHandler FLOAT = new TypeHandler("float") {
        @Override
        public Object decode(String value) { return Double.valueOf(value); }
    };

    public static final TypeHandler NUMBER = new TypeHandler("number") {
        @Override
        public Object decode(String value) { return Double.valueOf(value); }
    };

    public static final TypeHandler BOOLEAN = new TypeHandler("boolean") {
        @Override
        public Object decode(String value) { return Boolean.valueOf(value); }
    };

}
