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

package org.nuxeo.ecm.core.repository.jcr;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.AnyType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;
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
public final class TypeAdapter {

    private static final Type[] TYPE_MAP = {
        AnyType.INSTANCE, // undefined
        StringType.INSTANCE, // string
        BinaryType.INSTANCE, // binary
        LongType.INSTANCE,   // long
        DoubleType.INSTANCE, // double
        DateType.INSTANCE, // date
        BooleanType.INSTANCE, // boolean
        StringType.INSTANCE, // name
        StringType.INSTANCE, // path
        StringType.INSTANCE, // reference
    };

    private static final Map<String, Integer> SCALARS_2_JCR = new HashMap<String, Integer>();

    static {
        SCALARS_2_JCR.put(StringType.INSTANCE.getName(), PropertyType.STRING);
        SCALARS_2_JCR.put(BinaryType.INSTANCE.getName(), PropertyType.BINARY);
        SCALARS_2_JCR.put(LongType.INSTANCE.getName(), PropertyType.LONG);
        SCALARS_2_JCR.put(DoubleType.INSTANCE.getName(), PropertyType.DOUBLE);
        SCALARS_2_JCR.put(DateType.INSTANCE.getName(), PropertyType.DATE);
        SCALARS_2_JCR.put(BooleanType.INSTANCE.getName(), PropertyType.BOOLEAN);
        SCALARS_2_JCR.put(AnyType.INSTANCE.getName(), PropertyType.UNDEFINED);
    }

    private static final String DOCTYPE_PREFIX = NodeConstants.NS_ECM_DOCS_PREFIX + ':';
    private static final int DOCTYPE_PREFIX_LEN = DOCTYPE_PREFIX.length();

    private static final String PROPTYPE_PREFIX = NodeConstants.NS_ECM_FIELDS_PREFIX + ':';
    private static final int PROPTYPE_PREFIX_LEN = PROPTYPE_PREFIX.length();

    private static final String SCHEMA_PREFIX = NodeConstants.NS_ECM_SCHEMAS_PREFIX + ':';
    private static final int SCHEMA_PREFIX_LEN = SCHEMA_PREFIX.length();

    // This is an utility class.
    private TypeAdapter() {
    }

    public static Name getSchemaName(Schema schema) {
        return JCRName.NAME_FACTORY.create(NodeConstants.NS_ECM_SCHEMAS_URI, schema.getName());
    }

    public static Name getDocTypeName(DocumentType docType) {
        return JCRName.NAME_FACTORY.create(NodeConstants.NS_ECM_DOCS_URI, docType.getName());
    }

    public static Name getFieldTypeName(Type fieldType) {
        return JCRName.NAME_FACTORY.create(NodeConstants.NS_ECM_FIELDS_URI, fieldType.getName());
    }


    public static String docType2Jcr(String docTypeName) {
        return DOCTYPE_PREFIX + docTypeName;
    }

    public static String jcr2DocType(String jcrTypeName) {
        return jcrTypeName.substring(DOCTYPE_PREFIX_LEN);
    }

    public static boolean isJcrDocType(String jcrTypeName) {
        return jcrTypeName.startsWith(DOCTYPE_PREFIX);
    }

    public static String fieldType2Jcr(String typeName) {
        return PROPTYPE_PREFIX + typeName;
    }

    public static String jcr2FieldType(String jcrTypeName) {
        return jcrTypeName.substring(PROPTYPE_PREFIX_LEN);
    }

    public static boolean isJcrFieldType(String jcrTypeName) {
        return jcrTypeName.startsWith(PROPTYPE_PREFIX);
    }

    // document type conversions

    public static String docType2NodeType(String docType) {
        return DOCTYPE_PREFIX + docType;
    }

    public static String nodeType2DocType(String nt) {
        return nt.substring(DOCTYPE_PREFIX_LEN);
    }

    // schema type conversions

    public static String schema2NodeType(String schema) {
        return SCHEMA_PREFIX + schema;
    }

    public static String nodeType2Schema(String nt) {
        return nt.substring(SCHEMA_PREFIX_LEN);
    }

    // field type conversions

    public static String fieldType2NodeType(String type) {
        // hack to map content to nt:resource
//        if (type.equals(TypeConstants.CONTENT)) {
//            return "nt:resource";
//        }
        // the normal mapping
        return PROPTYPE_PREFIX + type;
    }

    public static String nodeType2FieldType(String nt) {
        // hack to map content to nt:resource
        if (nt.equals("nt:resource")) {
            return TypeConstants.CONTENT;
        }
        return nt.substring(PROPTYPE_PREFIX_LEN);
    }


    public static Type getFieldType(Property property) throws RepositoryException {
        int t = property.getType();
        return t < TYPE_MAP.length ? TYPE_MAP[t] : StringType.INSTANCE;
    }

    public static Type jcr2ScalarType(int type) {
        return type < TYPE_MAP.length ? TYPE_MAP[type] : StringType.INSTANCE;
    }

    // TODO store simple type constraints in JCR?
    // so a simple type will be converted into a primitive type and a set of constraints
    public static int scalarType2Jcr(Type type) {
        return SCALARS_2_JCR.get(((SimpleType) type).getPrimitiveType().getName());
    }

}
