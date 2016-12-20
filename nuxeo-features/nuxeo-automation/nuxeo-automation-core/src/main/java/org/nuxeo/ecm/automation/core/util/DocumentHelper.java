/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentHelper {

    private DocumentHelper() {
    }

    /**
     * Saves the document and clear context data to avoid incrementing version in next operations if not needed.
     */
    public static DocumentModel saveDocument(CoreSession session, DocumentModel doc) {
        doc = session.saveDocument(doc);
        return session.getDocument(doc.getRef());
    }

    /**
     * Removes a property from a document given the xpath. If the xpath points to a list property the list will be
     * cleared. If the path points to a blob in a list the property is removed from the list. Otherwise the xpath should
     * point to a non list property that will be removed.
     */
    public static void removeProperty(DocumentModel doc, String xpath) {
        Property p = doc.getProperty(xpath);
        if (p instanceof ListProperty) {
            ((ListProperty) p).clear();
        } else {
            Property pp = p.getParent();
            if (pp != null && pp.isList()) { // remove list entry
                ((ListProperty) pp).remove(p);
            } else {
                p.remove();
            }
        }
    }

    /**
     * Given a document property, updates its value with the given blob. The property can be a blob list or a blob. If a
     * blob list the blob is appended to the list, if a blob then it will be set as the property value. Both blob list
     * formats are supported: the file list (blob holder list) and simple blob list.
     */
    public static void addBlob(Property p, Blob blob) throws PropertyException {
        if (p.isList()) {
            // detect if a list of simple blobs or a list of files (blob
            // holder)
            Type ft = ((ListProperty) p).getType().getFieldType();
            if (ft.isComplexType() && ((ComplexType) ft).getFieldsCount() == 1) {
                p.addValue(createBlobHolderMap(blob));
            } else {
                p.addValue(blob);
            }
        } else {
            p.setValue(blob);
        }
    }

    public static HashMap<String, Serializable> createBlobHolderMap(Blob blob) {
        HashMap<String, Serializable> map = new HashMap<>();
        map.put("file", (Serializable) blob);
        return map;
    }

    public static void setProperties(CoreSession session, DocumentModel doc, Properties properties)
            throws IOException, PropertyException {
        if (properties instanceof DataModelProperties) {
            DataModelProperties dataModelProperties = (DataModelProperties) properties;
            for (Map.Entry<String, Serializable> entry : dataModelProperties.getMap().entrySet()) {
                doc.setPropertyValue(entry.getKey(), entry.getValue());
            }
        } else {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                setProperty(session, doc, key, value);
            }
        }
    }

    /**
     * Sets the properties given as a map of xpath:value to the given document. There is one special property: ecm:acl
     * that can be used to set the local acl. The format of this property value is: [string username]:[string
     * permission]:[boolean grant], [string username]:[string permission]:[boolean grant], ... TODO list properties are
     * not yet supported
     */
    public static void setProperties(CoreSession session, DocumentModel doc, Map<String, String> values)
            throws IOException {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            setProperty(session, doc, key, value);
        }
    }

    public static void setProperty(CoreSession session, DocumentModel doc, String key, String value)
            throws IOException {
        setProperty(session, doc, key, value, false);
    }

    protected static void setLocalAcl(CoreSession session, DocumentModel doc, String value) {
        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl(ACL.LOCAL_ACL);
        acp.addACL(acl);
        String[] entries = StringUtils.split(value, ',', true);
        if (entries.length == 0) {
            return;
        }
        for (String entry : entries) {
            String[] ace = StringUtils.split(entry, ':', true);
            acl.add(new ACE(ace[0], ace[1], Boolean.parseBoolean(ace[2])));
        }
        session.setACP(doc.getRef(), acp, false);
    }

    /**
     * Read an encoded string list as a comma separated list. To use comma inside list element values you need to escape
     * them using '\'. If the given type is different from {@link StringType#ID} then array elements will be converted
     * to the actual type.
     */
    public static Object readStringList(String value, SimpleType type) {
        if (!type.isPrimitive()) {
            return readStringList(value, type.getPrimitiveType());
        }
        String[] ar = readStringList(value);
        if (ar == null) {
            return null;
        }
        if (StringType.INSTANCE == type) {
            return ar;
        } else if (DateType.INSTANCE == type) {
            Calendar[] r = new Calendar[ar.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = (Calendar) type.decode(ar[i]);
            }
            return r;
        } else if (LongType.INSTANCE == type) {
            Long[] r = new Long[ar.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = (Long) type.decode(ar[i]);
            }
            return r;
        } else if (IntegerType.INSTANCE == type) {
            Integer[] r = new Integer[ar.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = (Integer) type.decode(ar[i]);
            }
            return r;
        } else if (DoubleType.INSTANCE == type) {
            Double[] r = new Double[ar.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = (Double) type.decode(ar[i]);
            }
            return r;
        } else if (BooleanType.INSTANCE == type) {
            Boolean[] r = new Boolean[ar.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = (Boolean) type.decode(ar[i]);
            }
            return r;
        } else if (BinaryType.INSTANCE == type) {
            InputStream[] r = new InputStream[ar.length];
            for (int i = 0; i < r.length; i++) {
                r[i] = (InputStream) type.decode(ar[i]);
            }
            return r;
        }
        throw new IllegalArgumentException(
                "Unsupported type when updating document properties from string representation: " + type);
    }

    /**
     * Read an encoded string list as a comma separated list. To use comma inside list element values you need to escape
     * them using '\'.
     */
    public static String[] readStringList(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() == 0) {
            return new String[0];
        }
        ArrayList<String> result = new ArrayList<>();
        char[] chars = value.toCharArray();
        StringBuilder buf = new StringBuilder();
        boolean esc = false;
        for (char c : chars) {
            if (c == '\\') {
                if (esc) {
                    buf.append('\\');
                    esc = false;
                } else {
                    esc = true;
                }
            } else if (c == ',') {
                if (esc) {
                    buf.append(',');
                    esc = false;
                } else {
                    result.add(buf.toString());
                    buf = new StringBuilder();
                }
            } else {
                buf.append(c);
            }
        }
        result.add(buf.toString());
        return result.toArray(new String[result.size()]);
    }

    /**
     * Sets the properties of a document based on their JSON representation (especially for scalar lists).
     *
     * @since 5.9.2
     */
    public static void setJSONProperties(CoreSession session, DocumentModel doc, Properties properties)
            throws IOException {

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            setProperty(session, doc, key, value, true);
        }
    }

    /**
     * @since 5.9.2
     */
    public static void setProperty(CoreSession session, DocumentModel doc, String key, String value,
            boolean decodeStringListAsJSON) throws IOException {
        if ("ecm:acl".equals(key)) {
            setLocalAcl(session, doc, value);
        }
        Property p = doc.getProperty(key);
        if (value == null || value.length() == 0) {
            p.setValue(null);
            return;
        }
        Type type = p.getField().getType();
        if (!type.isSimpleType()) {
            if (type.isListType()) {
                ListType ltype = (ListType) type;
                if (ltype.isScalarList() && !decodeStringListAsJSON) {
                    p.setValue(readStringList(value, (SimpleType) ltype.getFieldType()));
                    return;
                } else {
                    Object val = ComplexTypeJSONDecoder.decodeList(ltype, value);
                    p.setValue(val);
                    return;
                }
            } else if (type.isComplexType()) {
                Object val = ComplexTypeJSONDecoder.decode((ComplexType) type, value);
                p.setValue(val);
                return;
            }
            throw new NuxeoException("Property type is not supported by this operation");
        } else {
            p.setValue(((SimpleType) type).getPrimitiveType().decode(value));
        }
    }

}
