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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.json;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayout;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewLayoutImpl;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewState;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewStateImpl;
import org.nuxeo.ecm.platform.forms.layout.io.Base64;

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
     * @param encode if true, the resulting String will be zipped and encoded
     *            in Base-64 format.
     * @throws ClientException
     * @throws UnsupportedEncodingException
     */
    public static String toJSON(ContentViewState state, boolean encode)
            throws ClientException, UnsupportedEncodingException {
        if (state == null) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Encoding content view state: " + state);
        }

        // build json
        JSONObject jsonObject = new JSONObject();
        jsonObject.element("contentViewName", state.getContentViewName());
        jsonObject.element("pageSize", state.getPageSize());
        jsonObject.element("currentPage", state.getCurrentPage());

        JSONArray jsonQueryParams = new JSONArray();
        Object[] queryParams = state.getQueryParameters();
        if (queryParams != null) {
            // serialize to String before
            List<String> stringParams = new ArrayList<String>();
            for (Object queryParam : queryParams) {
                if (queryParam == null) {
                    stringParams.add(null);
                } else {
                    stringParams.add(queryParam.toString());
                }
            }
            jsonQueryParams.addAll(stringParams);
        }
        jsonObject.element("queryParameters", jsonQueryParams);

        jsonObject.element("searchDocument",
                getDocumentModelToJSON(state.getSearchDocumentModel()));

        JSONArray jsonSortInfos = new JSONArray();
        List<SortInfo> sortInfos = state.getSortInfos();
        if (sortInfos != null) {
            for (SortInfo sortInfo : sortInfos) {
                jsonSortInfos.add(getSortInfoToJSON(sortInfo));
            }
        }
        jsonObject.element("sortInfos", jsonSortInfos);

        jsonObject.element("resultLayout",
                getContentViewLayoutToJSON(state.getResultLayout()));

        List<String> resultColumns = state.getResultColumns();
        if (resultColumns != null) {
            jsonObject.element("resultColumns", resultColumns);
        }

        String jsonString = jsonObject.toString();

        if (log.isDebugEnabled()) {
            log.debug("Encoded content view state: " + jsonString);
        }

        // encoding
        if (encode) {
            String encodedValues = Base64.encodeBytes(jsonString.getBytes(),
                    Base64.GZIP | Base64.DONT_BREAK_LINES);
            jsonString = URLEncoder.encode(encodedValues,
                    ENCODED_VALUES_ENCODING);
        }
        return jsonString;
    }

    /**
     * Returns the content view state from its String serialization in JSON
     * format.
     *
     * @param json the state to de-serialize
     * @param decode if true, the input String is decoded from Base-64 format
     *            and unzipped.
     * @throws ClientException
     * @throws UnsupportedEncodingException
     */
    @SuppressWarnings("unchecked")
    public static ContentViewState fromJSON(String json, boolean decode)
            throws UnsupportedEncodingException, ClientException {
        if (json == null || json.trim().length() == 0) {
            return null;
        }
        // decoding
        if (decode) {
            String decodedValues = URLDecoder.decode(json,
                    ENCODED_VALUES_ENCODING);
            json = new String(Base64.decode(decodedValues));
        }

        if (log.isDebugEnabled()) {
            log.debug("Decoding content view state: " + json);
        }

        // parse json
        JSONObject jsonObject = JSONObject.fromObject(json);
        ContentViewState state = new ContentViewStateImpl();

        state.setContentViewName(jsonObject.getString("contentViewName"));
        state.setPageSize(Long.valueOf(jsonObject.optLong("pageSize", -1)));
        state.setCurrentPage(Long.valueOf(jsonObject.optLong("currentPage", -1)));

        JSONArray jsonQueryParams = jsonObject.getJSONArray("queryParameters");

        if (jsonQueryParams != null && !jsonQueryParams.isEmpty()) {
            List<Object> queryParams = new ArrayList<Object>();
            for (Object item : jsonQueryParams) {
                queryParams.add(item);
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

        if (log.isDebugEnabled()) {
            log.debug("Decoded content view state: " + state);
        }
        return state;
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

    protected static JSONObject getDocumentModelToJSON(DocumentModel doc)
            throws ClientException {
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
    protected static DocumentModel getDocumentModelFromJSON(JSONObject jsonDoc)
            throws ClientException {
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

    protected static JSONObject getContentViewLayoutToJSON(
            ContentViewLayout cvLayout) {
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

    protected static ContentViewLayout getContentViewLayoutFromJSON(
            JSONObject jsonCvLayout) {
        if (jsonCvLayout == null || jsonCvLayout.isNullObject()) {
            return null;
        }
        String name = jsonCvLayout.optString("name", null);
        String title = jsonCvLayout.optString("title", null);
        boolean translateTitle = jsonCvLayout.optBoolean("translateTitle");
        String iconPath = jsonCvLayout.optString("iconPath", null);
        boolean showCSVExport = jsonCvLayout.optBoolean("showCSVExport");
        return new ContentViewLayoutImpl(name, title, translateTitle, iconPath,
                showCSVExport);
    }

    @SuppressWarnings("unchecked")
    protected static Serializable getDocumentPropertyValue(Object o)
            throws JSONException {
        if (o instanceof String) {
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
                list.add(getDocumentPropertyValue(aJsonArray));
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
