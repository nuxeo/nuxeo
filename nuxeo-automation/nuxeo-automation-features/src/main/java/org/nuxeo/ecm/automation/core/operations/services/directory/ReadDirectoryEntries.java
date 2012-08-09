/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.automation.core.operations.services.directory;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

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
@Operation(id = ReadDirectoryEntries.ID, category = Constants.CAT_SERVICES, label = "Reads directory entries", description = "Reads directory entries. Entries ids to read are sent as a JSON array. Returns the entries as a JSON array of JSON objects containing all fields.")
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
    public Blob run() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        List<String> ids = mapper.readValue(jsonEntries,
                new TypeReference<List<String>>() {
                });
        List<Map<String, Object>> entries = new ArrayList<Map<String, Object>>();
        Session session = null;
        try {
            Directory directory = directoryService.getDirectory(directoryName);
            String schemaName = directory.getSchema();
            Schema schema = schemaManager.getSchema(schemaName);
            session = directoryService.open(directoryName);
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
        } finally {
            if (session != null) {
                session.close();
            }
        }

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, entries);
        return new InputStreamBlob(new ByteArrayInputStream(
                writer.toString().getBytes("UTF-8")), "application/json");
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
        return I18NUtils.getMessageString("messages", key, new Object[0],
                getLocale());
    }

}
