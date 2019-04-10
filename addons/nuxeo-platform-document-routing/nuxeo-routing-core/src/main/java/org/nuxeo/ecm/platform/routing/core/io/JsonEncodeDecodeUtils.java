/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.io;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.2
 */
public class JsonEncodeDecodeUtils {

    public static void encodeBlob(DocumentModel doc, String propVariableFacet, String variableName, Blob blob,
            JsonGenerator jg, ServletRequest request) throws JsonGenerationException, IOException {
        if (blob == null) {
            jg.writeNull();
            return;
        }
        jg.writeStartObject();
        String filename = blob.getFilename();
        if (filename == null) {
            jg.writeNullField("name");
        } else {
            jg.writeStringField("name", filename);
        }
        String v = blob.getMimeType();
        if (v == null) {
            jg.writeNullField("mime-type");
        } else {
            jg.writeStringField("mime-type", v);
        }
        v = blob.getEncoding();
        if (v == null) {
            jg.writeNullField("encoding");
        } else {
            jg.writeStringField("encoding", v);
        }
        v = blob.getDigest();
        if (v == null) {
            jg.writeNullField("digest");
        } else {
            jg.writeStringField("digest", v);
        }
        jg.writeStringField("length", Long.toString(blob.getLength()));

        String facet = null;
        try {
            facet = (String) doc.getPropertyValue(propVariableFacet);
        } catch (PropertyNotFoundException e) {
            facet = propVariableFacet;
        }
        if (StringUtils.isBlank(facet)) {
            return;
        }
        CompositeType type = Framework.getLocalService(SchemaManager.class).getFacet(facet);

        DownloadService downloadService = Framework.getService(DownloadService.class);
        String xpath = type.getField(variableName).getName().getPrefixedName();
        String blobUrl = VirtualHostHelper.getBaseURL(request) + downloadService.getDownloadUrl(doc, xpath, filename);
        jg.writeStringField("url", blobUrl);

        jg.writeEndObject();
    }

    @Deprecated
    public static Map<String, Serializable> decodeVariables(JsonNode jsnode,
            Map<String, Serializable> originalVariables, CoreSession session) throws ClassNotFoundException,
            IOException {
        Map<String, Serializable> variables = new HashMap<String, Serializable>();
        Iterator<Entry<String, JsonNode>> it = jsnode.getFields();
        while (it.hasNext()) {
            Entry<String, JsonNode> variable = it.next();
            String key = variable.getKey();
            JsonNode value = variable.getValue();
            if (value.isNumber()) {
                // We are working with String which will be correctly decoded by
                // org.nuxeo.ecm.platform.routing.core.impl.GraphVariablesUtil.setJSONVariables(DocumentModel, String,
                // Map<String, String>, boolean)
                // But we'll definitely need to convert submitted json variable to proper typed objects
                variables.put(key, value.getNumberValue().toString());
            } else if (value.isObject()) {
                if (value.has("upload-batch")) {
                    // Decoding of the blob will be handled in ComplexTypeJSONDecoder.decode
                    ObjectNode upload = (ObjectNode) value;
                    upload.put("type", "blob");
                    variables.put(key, upload.toString());
                }
            } else {
                variables.put(key, value.getTextValue());
            }

        }
        return variables;
    }

    public static void encodeVariableEntry(DocumentModel doc, String propVariableFacet, Entry<String, Serializable> e, JsonGenerator jg,
            HttpServletRequest request) throws JsonGenerationException, IOException {
        if (e.getValue() instanceof Blob) {
            jg.writeFieldName(e.getKey());
            JsonEncodeDecodeUtils.encodeBlob(doc, propVariableFacet, e.getKey(), (Blob) e.getValue(), jg,
                    request);
        } else {
            jg.writeObjectField(e.getKey(), e.getValue());
        }
    }
}
