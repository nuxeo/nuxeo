/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.doc;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JSONExporter {

    public static String toJSON() throws Exception {
        return toJSON(Framework.getService(AutomationService.class).getDocumentation());
    }

    public static void toJSON(Writer writer) throws Exception {
        toJSON(Framework.getService(AutomationService.class).getDocumentation(), writer);
    }

    public static String toJSON(List<OperationDocumentation> docs) throws IOException {
        StringWriter writer = new StringWriter();
        toJSON(docs, writer);
        return writer.toString();
    }
    
    public static void toJSON(List<OperationDocumentation> docs, Writer writer) throws IOException {
        JSONObject json = new JSONObject();
        JSONArray ops = new JSONArray();        
        for (OperationDocumentation doc : docs) {
            JSONObject op = toJSON(doc);            
            ops.add(op);
        }
        json.element("operations", ops);
        writer.write(json.toString(2));
    }

    protected static JSONObject toJSON(OperationDocumentation doc) throws IOException {
        JSONObject op = new JSONObject();
        op.element("id", doc.id);
        op.element("label", doc.label);
        op.element("category", doc.category);
        op.element("requires", doc.requires);
        op.element("description", doc.description);
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

}
