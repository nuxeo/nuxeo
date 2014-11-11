/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation.Param;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JSONExporter {
	
	// Utility class
	private JSONExporter() {
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

    public static JSONObject toJSON(OperationDocumentation doc) {
        JSONObject op = new JSONObject();
        op.element("id", doc.id);
        op.element("label", doc.label);
        op.element("category", doc.category);
        op.element("requires", doc.requires);
        op.element("description", doc.description);
        if (doc.since != null && doc.since.length() > 0) {
            op.element("since", doc.since);
        }
        op.element("url", doc.url);
        JSONArray sig = new JSONArray();
        for (String in : doc.signature) {
            sig.add(in);
        }
        op.element("signature", sig);
        JSONArray params = new JSONArray();
        for (Param p : doc.params) {
            JSONObject param = new JSONObject();
            param.element("name", p.name);
            param.element("type", p.type);
            param.element("required", p.isRequired);
            param.element("widget", p.widget);
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
        OperationDocumentation op = new OperationDocumentation(
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
                JSONObject p = params.getJSONObject(j);
                Param para = new Param();
                para.name = p.optString("name", null);
                para.type = p.optString("type", null);
                para.isRequired = p.optBoolean("required", false);
                para.widget = p.optString("widget", null);
                JSONArray ar = p.optJSONArray("values");
                if (ar != null) {
                    para.values = new String[ar.size()];
                    for (int k = 0, size2 = ar.size(); k < size2; k++) {
                        para.values[k] = ar.getString(k);
                    }
                }
                op.params.add(para);
            }
        }

        return op;
    }

}
