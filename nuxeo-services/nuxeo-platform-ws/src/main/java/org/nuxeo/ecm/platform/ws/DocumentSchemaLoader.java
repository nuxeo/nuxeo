/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.ws;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.api.ws.DocumentLoader;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;

/**
 * @author matic
 * 
 */
public class DocumentSchemaLoader implements DocumentLoader {

    @Override
    public void fillProperties(DocumentModel doc,
            List<DocumentProperty> props, WSRemotingSession rs)
            throws ClientException {
        String[] schemas = doc.getSchemas();
        for (String schema : schemas) {
            DataModel dm = doc.getDataModel(schema);
            Map<String, Object> map = dm.getMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                collectNoBlobProperty("", entry.getKey(), entry.getValue(),
                        props);
            }
        }
    }
    
    protected void collectNoBlobProperty(String prefix, String name,
            Object value, List<DocumentProperty> props) throws ClientException {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            prefix = prefix + name + '/';
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                collectNoBlobProperty(prefix, entry.getKey(), entry.getValue(),
                        props);
            }
        } else if (value instanceof List) {
            prefix = prefix + name + '/';
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            for (int i = 0, len = list.size(); i < len; i++) {
                collectNoBlobProperty(prefix, String.valueOf(i), list.get(i),
                        props);
            }
        } else if (!(value instanceof Blob)) {
            if (value == null) {
                props.add(new DocumentProperty(prefix + name, null));
            } else {
                collectProperty(prefix, name, value, props);
            }
        }
    }

   @SuppressWarnings("unchecked")
    protected void collectProperty(String prefix, String name, Object value,
            List<DocumentProperty> props) throws ClientException {
        final String STRINGS_LIST_SEP = ";";
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            prefix = prefix + name + '/';
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                collectProperty(prefix, entry.getKey(), entry.getValue(), props);
            }
        } else if (value instanceof List) {
            prefix = prefix + name + '/';
            List<Object> list = (List<Object>) value;
            for (int i = 0, len = list.size(); i < len; i++) {
                collectProperty(prefix, String.valueOf(i), list.get(i), props);
            }
        } else {
            String strValue = null;
            if (value != null) {
                if (value instanceof Blob) {
                    try {
                        // strValue = ((Blob) value).getString();
                        byte[] bytes = ((Blob) value).getByteArray();
                        strValue = Base64.encodeBytes(bytes);
                    } catch (IOException e) {
                        throw new ClientException(
                                "Failed to get blob property value", e);
                    }
                } else if (value instanceof Calendar) {
                    strValue = ((Calendar) value).getTime().toString();
                } else if (value instanceof String[]) {
                    for (String each : (String[]) value) {
                        if (strValue == null) {
                            strValue = each;
                        } else {
                            strValue = strValue + STRINGS_LIST_SEP + each;
                        }
                    }
                    // FIXME: this condition is always false here.
                } else if (value instanceof List) {
                    for (String each : (List<String>) value) {
                        if (strValue == null) {
                            strValue = each;
                        } else {
                            strValue = strValue + STRINGS_LIST_SEP + each;
                        }
                    }
                } else {
                    strValue = value.toString();
                } // TODO: use decode method from field type?
            }
            props.add(new DocumentProperty(prefix + name, strValue));
        }
    }


}
