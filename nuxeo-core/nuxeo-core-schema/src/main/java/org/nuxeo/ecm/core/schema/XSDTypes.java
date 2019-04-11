/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 */
public class XSDTypes {

    private static final Map<String, SimpleType> xsdBaseTypes = new HashMap<>();

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
        xsdBaseTypes.put("duration", StringType.INSTANCE);
        xsdBaseTypes.put("anyType", StringType.INSTANCE);
        xsdBaseTypes.put("anySimpleType", StringType.INSTANCE);
        xsdBaseTypes.put("anyURI", StringType.INSTANCE);
    }

    public static SimpleType getType(String name) {
        return xsdBaseTypes.get(name);
    }

    public static Collection<SimpleType> getTypes() {
        return xsdBaseTypes.values();
    }

}
