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
 */
package org.nuxeo.ecm.platform.picture.operation;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;

/**
 * Create a Picture document into the input document
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = CreatePicture.ID, category = Constants.CAT_SERVICES, label = "Create Picture", description = "Create a Picture document in the input folder. You can initialize the document properties using the 'properties' parameter. The properties are specified as <i>key=value</i> pairs separated by a new line. The key <i>originalPicture</i> is used to reference the JSON representation of the Blob for the original picture. The <i>pictureTemplates</i> parameter can be used to define the size of the different views to be generated, each line must be a JSONObject { title=\"title\", description=\"description\", maxsize=maxsize}. Returns the created document.")
public class CreatePicture {

    public static final String ID = "Picture.Create";

    public static final String PICTURE_FIELD = "originalPicture";

    @Context
    protected CoreSession session;

    @Param(name = "name", required = false)
    protected String name;

    @Param(name = "properties", required = false)
    protected Properties content;

    @Param(name = "pictureTemplates", required = false)
    protected Properties pictureTemplates;

    protected static final Log log = LogFactory.getLog(CreatePicture.class);

    protected ArrayList<Map<String, Object>> computePictureTemplates() {
        if (pictureTemplates == null || pictureTemplates.size() == 0) {
            return null;
        }
        ArrayList<Map<String, Object>> templates = new ArrayList<Map<String, Object>>();

        try {
            ObjectMapper mapper = new ObjectMapper();

            // for (String templateDef : pictureTemplates) {
            for (String name : pictureTemplates.keySet()) {
                String templateDef = pictureTemplates.get(name);
                JsonNode node = mapper.readTree(templateDef);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("tag", name);
                Iterator<Entry<String, JsonNode>> it = node.getFields();
                while (it.hasNext()) {
                    Entry<String, JsonNode> entry = it.next();
                    if (entry.getValue().isInt() || entry.getValue().isLong()) {
                        map.put(entry.getKey(), entry.getValue().getLongValue());
                    } else {
                        map.put(entry.getKey(), entry.getValue().getValueAsText());
                    }
                }
                templates.add(map);
            }
        } catch (IOException e) {
            log.error("Error while parsing picture templates", e);
        }

        return templates;
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws IOException {
        if (name == null) {
            name = "Untitled";
        }
        String jsonBlob = content.get(PICTURE_FIELD);
        content.remove(PICTURE_FIELD);

        ArrayList<Map<String, Object>> templates = computePictureTemplates();

        DocumentModel newDoc = session.createDocumentModel(doc.getPathAsString(), name, "Picture");
        if (content != null) {
            DocumentHelper.setProperties(session, newDoc, content);
        }
        DocumentModel picture = session.createDocument(newDoc);

        if (jsonBlob == null) {
            log.warn("Properties does not contains originalPicture field");
        } else {
            Blob blob = (Blob) ComplexTypeJSONDecoder.decode(null, jsonBlob);
            if (blob == null) {
                log.warn("Unable to read Blob from properties");
            } else {
                picture.setPropertyValue("file:content", (Serializable) blob);
                PictureResourceAdapter adapter = picture.getAdapter(PictureResourceAdapter.class);
                adapter.fillPictureViews(blob, blob.getFilename(), picture.getTitle(), templates);
                picture = session.saveDocument(picture);
            }
        }
        return picture;
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef doc) throws IOException {
        return run(session.getDocument(doc));
    }

}
