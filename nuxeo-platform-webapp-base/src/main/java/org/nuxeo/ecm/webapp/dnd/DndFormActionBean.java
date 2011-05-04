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
package org.nuxeo.ecm.webapp.dnd;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.schema.utils.DateParser;

/**
 * Seam action bean that is used to handle the meta-data form for the Drag&Drop
 * feature
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
@Name("dndFormActions")
@Scope(ScopeType.PAGE)
public class DndFormActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Map<String, Map<String, Serializable>> metadataCollector;

    protected String currentSchema;

    protected String currentLayout;

    @RequestParameter
    protected String schema;

    @RequestParameter
    protected String layout;

    public String getSchema() {
        if (schema != null && !schema.isEmpty()) {
            currentSchema = schema;
        }
        return currentSchema;
    }

    public String getLayout() {
        if (layout != null && !layout.isEmpty()) {
            currentLayout = layout;
        }
        return currentLayout;
    }

    @Factory(value = "dataCollector", scope = ScopeType.PAGE)
    public Map<String, Map<String, Serializable>> getCollector() {
        if (metadataCollector == null) {
            metadataCollector = new HashMap<String, Map<String, Serializable>>();
            metadataCollector.put(getSchema(),
                    new HashMap<String, Serializable>());
        }
        return metadataCollector;
    }

    public String save() throws JSONException {
        System.out.println("Saving DataCollector");
        for (String key : metadataCollector.keySet()) {
            System.out.println(key + " => "
                    + metadataCollector.get(key).toString());
        }
        sendHtmlJSONResponse();
        return null;
    }

    public void sendHtmlJSONResponse() throws JSONException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext econtext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) econtext.getResponse();

        response.setContentType("text/html");
        try {
            response.getWriter().write(getCollectedData());
            response.flushBuffer();
        } catch (IOException e) {

        }
        context.responseComplete();
    }

    public String getCollectedData() throws JSONException {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>\n");
        sb.append("<script>\n");
        sb.append("var collectedData= ");
        JSONObject jsonObject = new JSONObject();

        // Collect meta-data
        JSONObject formData = new JSONObject();
        for (String key : metadataCollector.keySet()) {
            for (String field : metadataCollector.get(key).keySet()) {
                Object data = metadataCollector.get(key).get(field);
                if (data instanceof Date) {
                    data = DateParser.formatW3CDateTime((Date) data);
                } else if (data instanceof Calendar) {
                    data = DateParser.formatW3CDateTime(((Calendar) data).getTime());
                }
                formData.put(key + ":" + field, data);
            }
        }
        jsonObject.put("docMetaData", formData);

        // Collect Tags
        // XXX

        sb.append(jsonObject.toString());
        sb.append(";\n");
        sb.append("//console.log(collectedData);\n");
        sb.append("window.parent.dndFormFunctionCB(collectedData);\n");
        sb.append("</script>\n");
        sb.append("</html>");

        return sb.toString();
    }
}
