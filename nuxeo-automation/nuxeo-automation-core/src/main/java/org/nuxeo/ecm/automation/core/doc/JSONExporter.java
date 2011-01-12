/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.core.doc;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.OperationDocumentationImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Exporter to JSON of documentation-related objects.
 */
public class JSONExporter {

    public static String toJSON() throws Exception {
        return toJSON(Framework.getService(AutomationService.class).getDocumentation());
    }

    public static void toJSON(Writer writer) throws Exception {
        toJSON(Framework.getService(AutomationService.class).getDocumentation(),
                writer);
    }

    public static String toJSON(List<OperationDocumentation> docs)
            throws IOException {
        StringWriter writer = new StringWriter();
        toJSON(docs, writer);
        return writer.toString();
    }

    public static void toJSON(List<OperationDocumentation> docs, Writer writer)
            throws IOException {
        JSONObject json = new JSONObject();
        JSONArray ops = new JSONArray();
        for (OperationDocumentation doc : docs) {
            JSONObject op = toJSON(doc);
            ops.add(op);
        }
        json.element("operations", ops);
        writer.write(json.toString(2));
    }

    public static JSONObject toJSON(OperationDocumentation doc)
            throws IOException {
        JSONObject op = new JSONObject();
        op.element("id", doc.getId());
        op.element("label", doc.getLabel());
        op.element("category", doc.getCategory());
        op.element("requires", doc.getRequires());
        op.element("description", doc.getDescription());
        if (doc.getSince() != null && doc.getSince().length() > 0) {
            op.element("since", doc.getSince());
        }
        op.element("url", doc.getUrl());
        JSONArray sig = new JSONArray();
        for (String in : doc.getSignature()) {
            sig.add(in);
        }
        op.element("signature", sig);
        JSONArray params = new JSONArray();
        for (Param p : doc.getParams()) {
            JSONObject param = new JSONObject();
            param.element("name", p.name);
            param.element("type", p.type);
            param.element("required", p.isRequired);
            param.element("widget", p.widget);
            param.element("order", p.order);
            JSONArray ar = new JSONArray();
            for (String value : p.values) {
                ar.add(value);
            }
            param.element("values", ar);
            params.add(param);
        }
        op.element("params", params);
        return op;
    }

    public static OperationDocumentation fromJSON(JSONObject json) {
        OperationDocumentationImpl op = new OperationDocumentationImpl(
                json.getString("id"));
        op.category = json.optString("label", null);
        op.category = json.optString("category", null);
        op.requires = json.optString("requires", null);
        op.description = json.optString("description", null);
        op.url = json.optString("url", op.id);
        JSONArray sig = json.optJSONArray("signature");
        if (sig != null) {
            op.signature = new String[sig.size()];
            for (int j = 0, size = sig.size(); j < size; j++) {
                op.signature[j] = sig.getString(j);
            }
        }
        // read params
        JSONArray params = json.optJSONArray("params");
        if (params != null) {
            op.params = new ArrayList<Param>(params.size());
            for (int j = 0, size = params.size(); j < size; j++) {
                String[] values;
                JSONObject p = params.getJSONObject(j);
                JSONArray ar = p.optJSONArray("values");
                if (ar == null) {
                    values = null;
                } else {
                    values = new String[ar.size()];
                    for (int k = 0, size2 = ar.size(); k < size2; k++) {
                        values[k] = ar.getString(k);
                    }
                }
                Param para = new Param(p.optString("name", null), p.optString(
                        "type", null), p.optString("widget", null), values,
                        p.optInt("order", 0), p.optBoolean("required", false));
                op.params.add(para);
            }
        }

        return op;
    }

}
