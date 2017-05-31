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
 *     qlamerand
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.Serializable;
import java.util.Locale;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * Return the content of a {@link Directory} as a JSON StringBlob
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 */
@Operation(id = GetDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Get directory entries", description = "Get the entries of a directory. This is returning a blob containing a serialized JSON array. The input document, if specified, is used as a context for a potential local configuration of the directory.", addToStudio = false)
public class GetDirectoryEntries {

    private static final Log log = LogFactory.getLog(GetDirectoryEntries.class);

    public static final String ID = "Directory.Entries";

    @Context
    protected OperationContext ctx;

    @Context
    protected DirectoryService directoryService;

    @Context
    protected SchemaManager schemaManager;

    @Param(name = "directoryName", required = true)
    protected String directoryName;

    @Param(name = "translateLabels", required = false)
    protected boolean translateLabels;

    @Param(name = "lang", required = false)
    protected String lang;

    @OperationMethod
    public Blob run(DocumentModel doc) {
        Directory directory = directoryService.getDirectory(directoryName, doc);
        try (Session session = directory.getSession()) {
            DocumentModelList entries = session.getEntries();
            String schemaName = directory.getSchema();
            Schema schema = schemaManager.getSchema(schemaName);
            JSONArray rows = new JSONArray();
            for (DocumentModel entry : entries) {
                JSONObject obj = new JSONObject();
                for (Field field : schema.getFields()) {
                    QName fieldName = field.getName();
                    String key = fieldName.getLocalName();
                    Serializable value = entry.getPropertyValue(fieldName.getPrefixedName());
                    if (translateLabels && "label".equals(key)) {
                        value = translate((String) value);
                    }
                    obj.element(key, value);
                }
                rows.add(obj);
            }
            return Blobs.createJSONBlob(rows.toString());
        }
    }

    @OperationMethod
    public Blob run() {
        return run(null);
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
