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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
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

    /**
     * @deprecated since 5.7
     */
    @Deprecated
    protected String currentSchema;

    /**
     * @deprecated since 5.7
     */
    @Deprecated
    protected String currentLayout;

    /**
     * @deprecated since 5.7
     */
    @RequestParameter
    @Deprecated
    protected String schema;

    /**
     * @deprecated since 5.7
     */
    @RequestParameter
    @Deprecated
    protected String layout;

    /**
     * @since 5.7
     */
    protected List<String> currentSchemas;

    /**
     * @since 5.7
     */
    protected String currentLayouts;

    /**
     * @since 5.7
     */
    @RequestParameter
    protected String schemas;

    /**
     * @since 5.7
     */
    @RequestParameter
    protected String layouts;

    /**
     * @deprecated since 5.7. Use {@link #getSchemas()}.
     */
    @Deprecated
    public String getSchema() {
        if (schema != null && !schema.isEmpty()) {
            currentSchema = schema;
        }
        return currentSchema;
    }

    /**
     * @deprecated since 5.7. Use {@link #getLayouts()}.
     */
    @Deprecated
    public String getLayout() {
        if (layout != null && !layout.isEmpty()) {
            currentLayout = layout;
        }
        return currentLayout;
    }

    public List<String> getSchemas() {
        currentSchemas = new ArrayList<>();
        if (StringUtils.isNotBlank(schemas)) {
            currentSchemas.addAll(Arrays.asList(schemas.split(",")));
        } else if (StringUtils.isNotBlank(schema)) {
            currentSchemas.add(schema);
        }
        return currentSchemas;
    }

    public String getLayouts() {
        if (StringUtils.isNotBlank(layouts)) {
            currentLayouts = layouts;
        } else if (StringUtils.isNotBlank(layout)) {
            currentLayouts = layout;
        }
        return currentLayouts;
    }

    @Factory(value = "dataCollector", scope = ScopeType.PAGE)
    public Map<String, Map<String, Serializable>> getCollector() {
        if (metadataCollector == null) {
            metadataCollector = new HashMap<String, Map<String, Serializable>>();
            for (String schema : getSchemas()) {
                metadataCollector.put(schema,
                        new HashMap<String, Serializable>());
            }
        }
        return metadataCollector;
    }

    public String save() throws JSONException {
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
