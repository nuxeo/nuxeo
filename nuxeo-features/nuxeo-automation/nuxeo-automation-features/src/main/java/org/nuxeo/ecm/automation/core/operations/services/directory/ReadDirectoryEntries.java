/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.services.directory;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads entries for a given {@link org.nuxeo.ecm.directory.Directory}.
 * <p>
 * Entries ids to read are sent as a JSON array.
 * <p>
 * Returns the entries as a JSON array of JSON objects containing all fields.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@Operation(id = ReadDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Reads directory entries", description = "Reads directory entries. Entries ids to read are sent as a JSON array. Returns the entries as a JSON array of JSON objects containing all fields.", addToStudio = false)
public class ReadDirectoryEntries extends AbstractDirectoryOperation {

    public static final String ID = "Directory.ReadEntries";

    @Context
    protected OperationContext ctx;

    @Context
    protected DirectoryService directoryService;

    @Context
    protected SchemaManager schemaManager;

    @Param(name = "directoryName", required = true)
    protected String directoryName;

    @Param(name = "entries", required = true)
    protected String jsonEntries;

    @Param(name = "translateLabels", required = false)
    protected boolean translateLabels;

    @Param(name = "lang", required = false)
    protected String lang;

    @OperationMethod
    public Blob run() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        List<String> ids = mapper.readValue(jsonEntries, new TypeReference<List<String>>() {
        });
        List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();

        Directory directory = directoryService.getDirectory(directoryName);
        String schemaName = directory.getSchema();
        Schema schema = schemaManager.getSchema(schemaName);
        try (Session session = directoryService.open(directoryName)) {
            for (String id : ids) {
                DocumentModel entry = session.getEntry(id);
                if (entry != null) {
                    Map<String, Object> m = new HashMap<String, Object>();
                    for (Field field : schema.getFields()) {
                        QName fieldName = field.getName();
                        String key = fieldName.getLocalName();
                        Serializable value = entry.getPropertyValue(fieldName.getPrefixedName());
                        if (translateLabels && "label".equals(key)) {
                            value = translate((String) value);
                        }
                        m.put(key, value);
                    }
                    entries.add(m);
                }
            }
        }

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, entries);
        return Blobs.createJSONBlob(writer.toString());
    }

    protected Locale getLocale() {
        if (lang == null) {
            lang = (String) ctx.get("lang");
        }
        if (lang == null) {
            lang = "en";
        }
        return new Locale(lang);
    }

    protected String translate(String key) {
        if (key == null) {
            return "";
        }
        return I18NUtils.getMessageString("messages", key, new Object[0], getLocale());
    }

}
