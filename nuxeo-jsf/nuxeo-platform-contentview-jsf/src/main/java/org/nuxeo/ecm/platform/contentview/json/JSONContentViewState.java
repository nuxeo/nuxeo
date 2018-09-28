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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.json;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.model.Literal;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayout;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayoutImpl;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewStateImpl;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

/**
 * Exporter/importer in JSON format of a {@link ContentViewState}.
 *
 * @since 5.4.2
 */
public class JSONContentViewState {

    private static final Log log = LogFactory.getLog(JSONContentViewState.class);

    public static final String ENCODED_VALUES_ENCODING = "UTF-8";

    /**
     * Returns the String serialization in JSON format of a content view state.
     *
     * @param state the state to serialize
     * @param encode if true, the resulting String will be zipped and encoded in Base-64 format.
     */
    public static String toJSON(ContentViewState state, boolean encode) throws IOException {
        if (state == null) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Encoding content view state: " + state);
        }

        // build json
        JSONObject jsonObject = new JSONObject();
        jsonObject.element("contentViewName", state.getContentViewName());
        jsonObject.element("pageProviderName", state.getPageProviderName());
        jsonObject.element("pageSize", state.getPageSize());
        jsonObject.element("currentPage", state.getCurrentPage());

        JSONArray jsonQueryParams = new JSONArray();
        Object[] queryParams = state.getQueryParameters();
        if (queryParams != null) {
            // NXP-10347 + NXP-17544: serialize to String all params that will be serialized to String by
            // NXQLQueryBuilder anyway, for consistency
            List<Object> serParams = new ArrayList<Object>();
            for (Object queryParam : queryParams) {
                if (queryParam == null) {
                    serParams.add(null);
                } else if (queryParam instanceof Object[] || queryParam instanceof Collection
                        || queryParam instanceof Boolean || queryParam instanceof Number
                        || queryParam instanceof Literal) {
                    serParams.add(queryParam);
                } else {
                    serParams.add(queryParam.toString());
                }
            }
            jsonQueryParams.addAll(serParams);
        }
        jsonObject.element("queryParameters", jsonQueryParams);

        jsonObject.element("searchDocument", getDocumentModelToJSON(state.getSearchDocumentModel()));

        JSONArray jsonSortInfos = new JSONArray();
        List<SortInfo> sortInfos = state.getSortInfos();
        if (sortInfos != null) {
            for (SortInfo sortInfo : sortInfos) {
                jsonSortInfos.add(getSortInfoToJSON(sortInfo));
            }
        }
        jsonObject.element("sortInfos", jsonSortInfos);

        jsonObject.element("resultLayout", getContentViewLayoutToJSON(state.getResultLayout()));

        List<String> resultColumns = state.getResultColumns();
        if (resultColumns != null) {
            jsonObject.element("resultColumns", resultColumns);
        }

        jsonObject.element("executed", state.isExecuted());

        String jsonString = jsonObject.toString();

        if (log.isDebugEnabled()) {
            log.debug("Encoded content view state: " + jsonString);
        }

        // encoding
        if (encode) {
            String encodedValues = base64GZIPEncoder(jsonString);
            jsonString = URLEncoder.encode(encodedValues, ENCODED_VALUES_ENCODING);
        }
        return jsonString;
    }

    /**
     * Returns the content view state from its String serialization in JSON format.
     *
     * @param json the state to de-serialize
     * @param decode if true, the input String is decoded from Base-64 format and unzipped.
     */
    @SuppressWarnings("unchecked")
    public static ContentViewState fromJSON(String json, boolean decode) throws IOException {
        if (json == null || json.trim().length() == 0) {
            return null;
        }
        // decoding
        if (decode) {
            String decodedValues = URLDecoder.decode(json, ENCODED_VALUES_ENCODING);
            json = base64GZIPDecoder(decodedValues);
        }

        if (log.isDebugEnabled()) {
            log.debug("Decoding content view state: " + json);
        }

        // parse json
        JSONObject jsonObject = JSONObject.fromObject(json);
        ContentViewState state = new ContentViewStateImpl();

        state.setContentViewName(jsonObject.getString("contentViewName"));
        state.setPageProviderName(jsonObject.optString("pageProviderName", null));
        state.setPageSize(Long.valueOf(jsonObject.optLong("pageSize", -1)));
        state.setCurrentPage(Long.valueOf(jsonObject.optLong("currentPage", -1)));

        JSONArray jsonQueryParams = jsonObject.getJSONArray("queryParameters");
        if (jsonQueryParams != null && !jsonQueryParams.isEmpty()) {
            List<Object> queryParams = new ArrayList<Object>();
            for (Object item : jsonQueryParams) {
                if (item instanceof JSONNull) {
                    queryParams.add(null);
                } else if (item instanceof JSONArray) {
                    queryParams.add(JSONArray.toCollection((JSONArray) item));
                } else {
                    queryParams.add(item);
                }
            }
            state.setQueryParameters(queryParams.toArray(new Object[queryParams.size()]));
        }

        JSONObject jsonDoc = jsonObject.getJSONObject("searchDocument");
        DocumentModel searchDoc = getDocumentModelFromJSON(jsonDoc);
        state.setSearchDocumentModel(searchDoc);

        JSONArray jsonSortInfos = jsonObject.getJSONArray("sortInfos");

        if (jsonSortInfos != null && !jsonSortInfos.isEmpty()) {
            List<SortInfo> sortInfos = new ArrayList<SortInfo>();
            for (Object item : jsonSortInfos) {
                sortInfos.add(getSortInfoFromJSON((JSONObject) item));
            }
            state.setSortInfos(sortInfos);
        }

        state.setResultLayout(getContentViewLayoutFromJSON(jsonObject.getJSONObject("resultLayout")));

        JSONArray jsonResultColumns = jsonObject.optJSONArray("resultColumns");
        if (jsonResultColumns != null) {
            List<String> resultColumns = new ArrayList<String>();
            resultColumns.addAll(jsonResultColumns);
            state.setResultColumns(resultColumns);
        }

        state.setExecuted(jsonObject.optBoolean("executed"));

        if (log.isDebugEnabled()) {
            log.debug("Decoded content view state: " + state);
        }

        return state;
    }

    protected static String base64GZIPEncoder(String value) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream b64os = Base64.getEncoder().wrap(baos); //
                OutputStream gzos = new GZIPOutputStream(b64os)) {
            IOUtils.write(value, gzos, UTF_8);
        }
        return new String(baos.toByteArray(), UTF_8);
    }

    protected static String base64GZIPDecoder(String value) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(value.getBytes(UTF_8));
        try (InputStream b64is = Base64.getDecoder().wrap(bais); //
                InputStream gzis = new GZIPInputStream(b64is)) {
            return IOUtils.toString(gzis, UTF_8);
        }
    }

    protected static JSONObject getSortInfoToJSON(SortInfo sortInfo) {
        JSONObject res = new JSONObject();
        res.element(SortInfo.SORT_COLUMN_NAME, sortInfo.getSortColumn());
        res.element(SortInfo.SORT_ASCENDING_NAME, sortInfo.getSortAscending());
        return res;
    }

    protected static SortInfo getSortInfoFromJSON(JSONObject jsonSortInfo) {
        String sortColumn = jsonSortInfo.getString(SortInfo.SORT_COLUMN_NAME);
        boolean sortAscending = jsonSortInfo.getBoolean(SortInfo.SORT_ASCENDING_NAME);
        return new SortInfo(sortColumn, sortAscending);
    }

    protected static JSONObject getDocumentModelToJSON(DocumentModel doc) {
        if (doc == null) {
            return null;
        }
        JSONObject res = new JSONObject();
        res.element("type", doc.getType());
        JSONObject props = (new DocumentModelToJSON()).run(doc);
        res.element("properties", props);
        return res;
    }

    @SuppressWarnings("unchecked")
    protected static DocumentModel getDocumentModelFromJSON(JSONObject jsonDoc) {
        if (jsonDoc == null || jsonDoc.isNullObject()) {
            return null;
        }
        String docType = jsonDoc.getString("type");
        DocumentModel doc = DocumentModelFactory.createDocumentModel(docType);
        JSONObject props = jsonDoc.getJSONObject("properties");
        Iterator<String> keys = props.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            doc.setPropertyValue(key, getDocumentPropertyValue(props.get(key)));
        }
        return doc;
    }

    protected static JSONObject getContentViewLayoutToJSON(ContentViewLayout cvLayout) {
        if (cvLayout == null) {
            return null;
        }
        JSONObject res = new JSONObject();
        res.element("name", cvLayout.getName());
        res.element("title", cvLayout.getTitle());
        res.element("translateTitle", cvLayout.getTranslateTitle());
        res.element("iconPath", cvLayout.getIconPath());
        res.element("showCSVExport", cvLayout.getShowCSVExport());
        return res;
    }

    protected static ContentViewLayout getContentViewLayoutFromJSON(JSONObject jsonCvLayout) {
        if (jsonCvLayout == null || jsonCvLayout.isNullObject()) {
            return null;
        }
        String name = jsonCvLayout.optString("name", null);
        String title = jsonCvLayout.optString("title", null);
        boolean translateTitle = jsonCvLayout.optBoolean("translateTitle");
        String iconPath = jsonCvLayout.optString("iconPath", null);
        boolean showCSVExport = jsonCvLayout.optBoolean("showCSVExport");
        return new ContentViewLayoutImpl(name, title, translateTitle, iconPath, showCSVExport);
    }

    @SuppressWarnings("unchecked")
    protected static Serializable getDocumentPropertyValue(Object o) throws JSONException {
        if (o == null || o instanceof JSONNull) {
            return null;
        } else if (o instanceof String) {
            Calendar calendar = null;
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ");
                Date date = df.parse((String) o);
                calendar = Calendar.getInstance();
                calendar.setTime(date);
            } catch (ParseException e) {
            }

            if (calendar != null) {
                return calendar;
            } else {
                return (Serializable) o;
            }
        } else if (o instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) o;
            ArrayList<Serializable> list = new ArrayList<Serializable>();
            for (Object aJsonArray : jsonArray) {
                Serializable pValue = getDocumentPropertyValue(aJsonArray);
                if (pValue != null) {
                  list.add(pValue);
                }
            }
            return list;
        } else if (o instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) o;
            HashMap<String, Serializable> map = new HashMap<String, Serializable>();
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, getDocumentPropertyValue(jsonObject.get(key)));
            }
            return map;
        } else {
            return (Serializable) o;
        }
    }

}
