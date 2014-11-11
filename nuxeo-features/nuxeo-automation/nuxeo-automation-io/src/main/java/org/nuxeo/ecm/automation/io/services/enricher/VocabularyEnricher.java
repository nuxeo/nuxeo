/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This enricher adds the labels for each value of a property referencing
 * dbl10n vocabularies.
 *
 * @since 6.0
 */
public class VocabularyEnricher extends AbstractContentEnricher {

    protected static final Log log = LogFactory.getLog(VocabularyEnricher.class);

    public static final String DIRECTORY_DEFAULT_LABEL_PREFIX = "label_";

    public static final String KEY_SEPARATOR = "/";

    // Parameters keys
    public static final String FIELD_PARAMETER = "field";

    public static final String DIRECTORY_PARAMETER = "directoryName";

    // Parameters values
    private String field;

    private String directoryName;

    private DirectoryService directoryService;

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec)
            throws ClientException, IOException {

        final DocumentModel doc = ec.getDocumentModel();

        jg.writeStartObject();

        try {

            // Lookup directory schema to find label columns
            List<String> labelFields = getLabelFields(directoryName);

            // Get the field values
            String[] entriesIds = getPropertyValues(doc, field);

            // 'field': [
            jg.writeFieldName(field);
            jg.writeStartArray();

            // { 'id': ..., 'label_*': ... }
            if (entriesIds != null) {
                writeLabels(jg, directoryName, entriesIds, labelFields);
            }

            // ]
            jg.writeEndArray();

        } catch (PropertyNotFoundException | DirectoryException e) {
            log.error(e.getMessage());
        }

        jg.writeEndObject();
        jg.flush();
    }


    @Override
    public void setParameters(Map<String, String> parameters) {
        field = parameters.get(FIELD_PARAMETER);
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException(
                "Parameter '" + FIELD_PARAMETER + "' cannot be empty");
        }
        directoryName = parameters.get(DIRECTORY_PARAMETER);
        if (directoryName == null || directoryName.isEmpty()) {
            throw new IllegalArgumentException(
                "Parameter '" + DIRECTORY_PARAMETER + "' cannot be empty");
        }
    }

    /**
     * Writes the labels for each entry
     * @param jg
     * @param directoryName
     * @param entriesIds
     * @param labelFields
     * @throws IOException
     */
    private void writeLabels(JsonGenerator jg, String directoryName,
        String[] entriesIds, List<String> labelFields) throws IOException {
        Session session = null;
        try {
            session = getDirectoryService().open(directoryName);
            for (String entryId : entriesIds) {
                Map<String, String> labels =
                    getAbsoluteLabels(entryId, session, labelFields);
                // Write absolute labels (<parent label> / <child label>)
                jg.writeStartObject();
                jg.writeStringField("id", entryId);
                for (Map.Entry<String, String> label : labels.entrySet()) {
                    jg.writeStringField(label.getKey(),
                        label.getValue());
                }
                jg.writeEndObject();
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }

    }

    /**
     * Determines label columns based on the label prefix
     * @param directoryName the name of the directory to inspect
     * @return
     */
    private List<String> getLabelFields(String directoryName) {
        String schemaName = getDirectoryService().getDirectorySchema(
            directoryName);

        SchemaManager schemaManager = Framework.getLocalService(
            SchemaManager.class);

        Schema schema = schemaManager.getSchema(schemaName);

        List<String> labelFields = new ArrayList<>();
        String fieldName;
        for (Field field : schema.getFields()) {
            fieldName = field.getName().toString();
            if (fieldName.startsWith(DIRECTORY_DEFAULT_LABEL_PREFIX)) {
                labelFields.add(fieldName);
            }
        }

        return labelFields;
    }

    private DirectoryService getDirectoryService() {
        if (directoryService == null) {
            directoryService = Framework.getLocalService(
                DirectoryService.class);
        }
        return directoryService;

    }

    /**
     * Return the values of a document's property as an array of strings
     * @param doc
     * @param fieldName
     * @return
     */
    private static String[] getPropertyValues(DocumentModel doc, String fieldName) {
        String[] entriesIds = null;
        Property prop = doc.getProperty(fieldName);
        if (prop.isList()) {
            entriesIds = prop.getValue(String[].class);
        } else {
            String value = prop.getValue(String.class);
            if (value != null) {
                entriesIds = new String[] { value };
            }
        }
        return entriesIds;
    }

    /**
     * Returns absolute labels for a given entry (<parent label> / <child label>)
     * @param entryId
     * @param session
     * @param labelFields
     * @return a map of field: label
     * @throws ClientException
     */
    private static Map<String, String> getAbsoluteLabels(final String entryId,
            final Session session, List<String> labelFields) throws ClientException {

        String[] split = entryId.split(KEY_SEPARATOR);
        Map<String, String> labels = new HashMap<>();

        for (int i = 0; i < split.length; i++) {
            DocumentModel entry = session.getEntry(split[i]);
            if (entry == null) {
                continue;
            }

            for (String labelField : labelFields) {
                String result = labels.get(labelField);
                if (result == null) {
                    result = "";
                }
                String value = (String) entry.getPropertyValue(labelField);
                result += (i > 0 ? "/" : "") + value;
                labels.put(labelField, result);
            }

        }

        return labels;
    }

}
