/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.syndication.serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.syndication.translate.TranslationHelper;
import org.restlet.data.MediaType;
import org.restlet.data.Response;

public class DMJSONSerializer extends AbstractDocumentModelSerializer {

    @Override
    public String serialize(ResultSummary summary, DocumentModelList docList,
            List<String> columnsDefinition, HttpServletRequest req)
            throws ClientException {
        return serialize(summary, docList, columnsDefinition, req, null, null);
    }

    public String serialize(ResultSummary summary, DocumentModelList docList,
            List<String> columnsDefinition, HttpServletRequest req,
            List<String> labels, String lang) throws ClientException {

        if (docList == null) {
            return EMPTY_LIST;
        }

        Map<String, Object> all = new HashMap<String, Object>();

        all.put("summary", summary);

        List<Map<String, String>> struct = new ArrayList<Map<String, String>>();

        for (DocumentModel doc : docList) {
            Map<String, String> resDoc = new HashMap<String, String>();

            resDoc.put("id", doc.getId());

            for (String colDef : columnsDefinition) {
                ResultField res = getDocumentProperty(doc, colDef);
                resDoc.put(res.getName(), res.getValue());
            }
            struct.add(resDoc);
        }

        all.put("data", struct);

        // add translations if asked for
        if (lang != null && labels != null) {
            Map<String, String> translations = new HashMap<String, String>();
            for (String key : labels) {
                translations.put(key, TranslationHelper.getLabel(key, lang));
            }
            all.put("translations", translations);
        }

        return makeJSON(all);
    }

    protected static String makeJSON(Map<String, Object> all) {
        JSON jsonRes = JSONSerializer.toJSON(all);
        if (jsonRes instanceof JSONObject) {
            JSONObject jsonOb = (JSONObject) jsonRes;
            return jsonOb.toString(2);
        } else if (jsonRes instanceof JSONArray) {
            JSONArray jsonOb = (JSONArray) jsonRes;
            return jsonOb.toString(2);
        } else {
            return null;
        }
    }

    @Override
    public void serialize(ResultSummary summary, DocumentModelList docList,
            String columnsDefinition, Response res, HttpServletRequest req)
            throws ClientException {
        String json = serialize(summary, docList, columnsDefinition, req);
        res.setEntity(json, MediaType.TEXT_PLAIN);
    }

    @Override
    public void serialize(ResultSummary summary, DocumentModelList docList,
            String columnsDefinition, Response res, HttpServletRequest req,
            List<String> labels, String lang) throws ClientException {
        List<String> cols = new ArrayList<String>();
        if (columnsDefinition != null) {
            cols = Arrays.asList(columnsDefinition.split(colDefinitonDelimiter));
        }
        String json = serialize(summary, docList, cols, req, labels, lang);
        res.setEntity(json, MediaType.TEXT_PLAIN);
    }

}
