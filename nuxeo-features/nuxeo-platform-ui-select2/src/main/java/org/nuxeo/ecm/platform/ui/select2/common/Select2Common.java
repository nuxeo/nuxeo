/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.platform.ui.select2.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.features.SuggestConstants;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Group fields and methods used at initialization and runtime for select2 feature.
 *
 * @since 5.7.3
 */
public class Select2Common extends SuggestConstants {

    private static final Log log = LogFactory.getLog(Select2Common.class);

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // no instantiation
    private Select2Common() {
    }

    public static final String LOCKED = "locked";

    public static final String PLACEHOLDER = "placeholder";

    public static final List<String> SELECT2_USER_WIDGET_TYPE_LIST = new ArrayList<String>(
            Arrays.asList("singleUserSuggestion", "multipleUsersSuggestion"));

    public static final List<String> SELECT2_DOC_WIDGET_TYPE_LIST = new ArrayList<String>(
            Arrays.asList("singleDocumentSuggestion", "multipleDocumentsSuggestion"));

    public static final String SUGGESTION_FORMATTER = "suggestionFormatter";

    public static final String SELECTION_FORMATTER = "selectionFormatter";

    public static final String USER_DEFAULT_SUGGESTION_FORMATTER = "userEntryDefaultFormatter";

    public static final String DOC_DEFAULT_SUGGESTION_FORMATTER = "docEntryDefaultFormatter";

    public static final List<String> SELECT2_DIR_WIDGET_TYPE_LIST = new ArrayList<String>(
            Arrays.asList("suggestOneDirectory", "suggestManyDirectory"));

    public static final List<String> SELECT2_DEFAULT_DOCUMENT_SCHEMAS = new ArrayList<String>(
            Arrays.asList("dublincore", "common"));

    public static final String DIR_DEFAULT_SUGGESTION_FORMATTER = "dirEntryDefaultFormatter";

    public static final String READ_ONLY_PARAM = "readonly";

    public static final String RERENDER_JS_FUNCTION_NAME = "reRenderFunctionName";

    public static final String AJAX_RERENDER = "ajaxReRender";

    public static final String USER_DEFAULT_SELECTION_FORMATTER = "userSelectionDefaultFormatter";

    public static final String DOC_DEFAULT_SELECTION_FORMATTER = "docSelectionDefaultFormatter";

    public static final String DIR_DEFAULT_SELECTION_FORMATTER = "dirSelectionDefaultFormatter";

    public static final String WIDTH = "width";

    public static final String DEFAULT_WIDTH = "300";

    public static final String MIN_CHARS = "minChars";

    public static final int DEFAULT_MIN_CHARS = 3;

    public static final String TITLE = "title";

    public static final String OPERATION_ID = "operationId";

    /**
     * @since 5.9.3
     */
    public static String[] getDefaultSchemas() {
        return getSchemas(null);
    }

    /**
     * Returns an array containing the given schema names plus the default ones if not included
     *
     * @param schemaNames
     * @since 5.8
     */
    public static String[] getSchemas(final String schemaNames) {
        List<String> result = new ArrayList<String>();
        result.addAll(Select2Common.SELECT2_DEFAULT_DOCUMENT_SCHEMAS);
        String[] temp = null;
        if (schemaNames != null && !schemaNames.isEmpty()) {
            temp = schemaNames.split(",");
        }
        if (temp != null) {
            for (String s : temp) {
                result.add(s);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * @since 6.0
     */
    public static String resolveDefaultEntries(final List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        } else {
            try {
                List<Map<String, Object>> result = new ArrayList<>();
                for (String l : list) {
                    Map<String, Object> obj = new LinkedHashMap<>();
                    obj.put(Select2Common.ID, l);
                    obj.put(Select2Common.LABEL, l);
                    result.add(obj);
                }
               return OBJECT_MAPPER.writeValueAsString(result);
            } catch (IOException e) {
                throw new NuxeoException("Unable to serialize json", e);
            }
        }
    }

    /**
     * Finds a document by the given property and value. If the property is null or empty the value is considered as a
     * {@link DocumentRef}.
     *
     * @since 9.2
     */
    public static DocumentModel resolveReference(String property, String value, CoreSession session) {
        if (property != null && !property.isEmpty()) {
            String query = "select * from Document where " + property + "=" + NXQL.escapeString(value);
            DocumentModelList docs = session.query(query);
            if (docs.size() > 0) {
                return docs.get(0);
            }
            log.warn("Unable to resolve doc using property " + property + " and value " + value);
            return null;
        }
        DocumentRef ref = null;
        if (value.startsWith("/")) {
            ref = new PathRef(value);
        } else {
            ref = new IdRef(value);
        }
        if (session.exists(ref)) {
            return session.getDocument(ref);
        }
        log.warn("Unable to resolve reference on " + ref);
        return null;
    }

}
