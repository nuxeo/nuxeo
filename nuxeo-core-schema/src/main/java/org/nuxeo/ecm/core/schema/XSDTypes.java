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

package org.nuxeo.ecm.core.schema;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XSDTypes {

    private static final Map<String, SimpleType> xsdBaseTypes = new HashMap<String, SimpleType>();

    // Utility class.
    private XSDTypes() {
    }

    static {
        xsdBaseTypes.put("string", StringType.INSTANCE);
        xsdBaseTypes.put("normalizedString", StringType.INSTANCE);
        xsdBaseTypes.put("integer", LongType.INSTANCE);
        xsdBaseTypes.put("long", LongType.INSTANCE);
        xsdBaseTypes.put("int", LongType.INSTANCE);
        xsdBaseTypes.put("short", LongType.INSTANCE);
        xsdBaseTypes.put("unsignedShort", LongType.INSTANCE);
        xsdBaseTypes.put("positiveInteger", LongType.INSTANCE);
        xsdBaseTypes.put("nonPositiveInteger", LongType.INSTANCE);
        xsdBaseTypes.put("nonNegativeInteger", LongType.INSTANCE);
        xsdBaseTypes.put("unsignedInt", LongType.INSTANCE);
        xsdBaseTypes.put("unsignedLong", LongType.INSTANCE);
        xsdBaseTypes.put("decimal", DoubleType.INSTANCE);
        xsdBaseTypes.put("double", DoubleType.INSTANCE);
        xsdBaseTypes.put("float", DoubleType.INSTANCE);
        xsdBaseTypes.put("date", DateType.INSTANCE);
        xsdBaseTypes.put("dateTime", DateType.INSTANCE);
        xsdBaseTypes.put("time", DateType.INSTANCE);
        xsdBaseTypes.put("boolean", BooleanType.INSTANCE);
        xsdBaseTypes.put("base64Binary", BinaryType.INSTANCE);
        xsdBaseTypes.put("hexBinary", BinaryType.INSTANCE);
    }

    static public SimpleType getType(String name) {
        return xsdBaseTypes.get(name);
    }

    public static Collection<SimpleType> getTypes() {
        return xsdBaseTypes.values();
    }

}
