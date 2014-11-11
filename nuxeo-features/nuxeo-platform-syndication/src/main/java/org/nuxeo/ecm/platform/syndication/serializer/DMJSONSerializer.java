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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.syndication.serializer;

import java.util.ArrayList;
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
import org.restlet.data.MediaType;
import org.restlet.data.Response;

public class DMJSONSerializer extends AbstractDocumentModelSerializer {

    @Override
    public String serialize(ResultSummary summary, DocumentModelList docList,
            List<String> columnsDefinition, HttpServletRequest req) throws ClientException {

        if (docList == null) {
            return EMPTY_LIST;
        }

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

        JSON jsonRes = JSONSerializer.toJSON(struct);

        if (jsonRes instanceof JSONObject) {
            JSONObject jsonOb = (JSONObject) jsonRes;
            return jsonOb.toString(1);
        } else if (jsonRes instanceof JSONArray) {
            JSONArray jsonOb = (JSONArray) jsonRes;
            return jsonOb.toString(1);
        } else {
            return null;
        }
    }

    @Override
    public void serialize(ResultSummary summary, DocumentModelList docList,
            String columnsDefinition, Response res, HttpServletRequest req) throws ClientException {
        String json = serialize(summary, docList, columnsDefinition, req);
        res.setEntity(json, MediaType.TEXT_PLAIN);
    }

}
