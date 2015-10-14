/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 *     Thierry Martins
 */
package org.nuxeo.ecm.webapp.dnd;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam action bean that is used to handle the meta-data form for the Drag&Drop feature
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Name("dndFormActions")
@Scope(ScopeType.PAGE)
public class DndFormActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected DocumentModel metadataCollector;

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

    public List<String> getSchemas() {
        currentSchemas = new ArrayList<>();
        if (StringUtils.isNotBlank(schemas)) {
            SchemaManager sc = Framework.getLocalService(SchemaManager.class);
            for (String schemaName : schemas.split(",")) {
                Schema schema = sc.getSchemaFromPrefix(schemaName);
                if (schema != null) {
                    currentSchemas.add(schema.getName());
                } else {
                    currentSchemas.add(schemaName);
                }
            }
        }
        return currentSchemas;
    }

    public String getLayouts() {
        if (StringUtils.isNotBlank(layouts)) {
            currentLayouts = layouts;
        }
        return currentLayouts;
    }

    @Factory(value = "dataCollector", scope = ScopeType.PAGE)
    public DocumentModel getCollector() {
        if (metadataCollector == null) {
            metadataCollector = new SimpleDocumentModel(getSchemas());
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

        DocumentModel collector = getCollector();
        // Collect meta-data
        JSONObject formData = new JSONObject();
        for (String key : collector.getSchemas()) {
            DataModel dataModel = collector.getDataModel(key);
            for (String field : dataModel.getDirtyFields()) {
                Object data = dataModel.getData(field);
                if (data instanceof Date) {
                    data = DateParser.formatW3CDateTime((Date) data);
                } else if (data instanceof Calendar) {
                    data = DateParser.formatW3CDateTime(((Calendar) data).getTime());
                }
                formData.put(field, data);
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