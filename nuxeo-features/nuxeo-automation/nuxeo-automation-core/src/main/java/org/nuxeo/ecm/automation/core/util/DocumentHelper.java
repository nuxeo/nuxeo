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
 */
package org.nuxeo.ecm.automation.core.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentHelper {

    /**
     * Saves the document and clear context data to avoid incrementing version
     * in next operations if not needed.
     */
    public static DocumentModel saveDocument(CoreSession session,
            DocumentModel doc) throws ClientException {
        doc = session.saveDocument(doc);
        return session.getDocument(doc.getRef());
    }

    /**
     * Removes a property from a document given the xpath. If the xpath points
     * to a list property the list will be cleared. If the path points to a
     * blob in a list the property is removed from the list. Otherwise the
     * xpath should point to a non list property that will be removed.
     */
    public static void removeProperty(DocumentModel doc, String xpath)
            throws ClientException {
        Property p = doc.getProperty(xpath);
        if (p.isList()) {
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
     * Given a document property, updates its value with the given blob. The
     * property can be a blob list or a blob. If a blob list the blob is
     * appended to the list, if a blob then it will be set as the property
     * value. Both blob list formats are supported: the file list (blob holder
     * list) and simple blob list.
     */
    public static void addBlob(Property p, Blob blob) throws PropertyException {
        if (p.isList()) {
            // detect if a list of simple blobs or a list of files (blob
            // holder)
            Type ft = ((ListProperty) p).getType().getFieldType();
            if (ft.isComplexType() && ((ComplexType) ft).getFieldsCount() == 2) {
                p.addValue(createBlobHolderMap(blob));
            } else {
                p.addValue(blob);
            }
        } else {
            p.setValue(blob);
        }
    }

    public static HashMap<String, Serializable> createBlobHolderMap(Blob blob) {
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("file", (Serializable) blob);
        map.put("filename", blob.getFilename());
        return map;
    }

    /**
     * Sets the properties given as a map of xpath:value to the given document.
     * There is one special property: ecm:acl that can be used to set the local
     * acl. The format of this property value is: [string username]:[string
     * permission]:[boolean grant], [string username]:[string
     * permission]:[boolean grant], ... TODO list properties are not yet
     * supported
     */
    public static void setProperties(CoreSession session, DocumentModel doc,
            Map<String, String> values) throws Exception {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("ecm:acl".equals(key)) {
                setLocalAcl(session, doc, value);
            }
            Property p = doc.getProperty(key);
            Type type = p.getField().getType();
            if (!type.isSimpleType()) {
                throw new OperationException(
                        "Only scalar types can be set using update operation");
            }
            if (value == null || value.length() == 0) {
                p.setValue(null);
            } else {
                p.setValue(((SimpleType) type).getPrimitiveType().decode(value));
            }
        }
    }

    protected static void setLocalAcl(CoreSession session, DocumentModel doc,
            String value) throws ClientException {
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

}
