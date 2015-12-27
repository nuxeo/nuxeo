/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.platform.ws;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.api.ws.DocumentLoader;
import org.nuxeo.ecm.platform.api.ws.DocumentProperty;
import org.nuxeo.ecm.platform.api.ws.session.WSRemotingSession;

/**
 * @author matic
 */
public class DocumentSchemaLoader implements DocumentLoader {

    @Override
    public void fillProperties(DocumentModel doc, List<DocumentProperty> props, WSRemotingSession rs)
            {
        String[] schemas = doc.getSchemas();
        for (String schema : schemas) {
            DataModel dm = doc.getDataModel(schema);
            Map<String, Object> map = dm.getMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                collectNoBlobProperty("", entry.getKey(), entry.getValue(), props);
            }
        }
    }

    protected void collectNoBlobProperty(String prefix, String name, Object value, List<DocumentProperty> props)
            {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            prefix = prefix + name + '/';
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                collectNoBlobProperty(prefix, entry.getKey(), entry.getValue(), props);
            }
        } else if (value instanceof List) {
            prefix = prefix + name + '/';
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            for (int i = 0, len = list.size(); i < len; i++) {
                collectNoBlobProperty(prefix, String.valueOf(i), list.get(i), props);
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
    protected void collectProperty(String prefix, String name, Object value, List<DocumentProperty> props)
            {
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
                        strValue = Base64.encodeBase64String(bytes);
                    } catch (IOException e) {
                        throw new NuxeoException("Failed to get blob property value", e);
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
