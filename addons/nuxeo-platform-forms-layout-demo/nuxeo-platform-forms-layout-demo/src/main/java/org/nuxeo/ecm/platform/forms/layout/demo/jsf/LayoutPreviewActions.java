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
package org.nuxeo.ecm.platform.forms.layout.demo.jsf;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;
import org.nuxeo.ecm.platform.forms.layout.io.JSONLayoutExporter;

import static org.jboss.seam.ScopeType.EVENT;

/**
 * Seam component handling preview.
 *
 * @since 5.4.2
 */
@Name("layoutPreviewActions")
@Scope(EVENT)
public class LayoutPreviewActions {

    private static final Log log = LogFactory.getLog(LayoutPreviewActions.class);

    // XXX: use hard coded JSF category for now
    public static final String LAYOUT_CATEGORY = "jsf";

    public String getPreviewLayoutURL(PreviewLayoutDefinition previewLayoutDef, String layoutMode, String layoutTemplate)
            throws UnsupportedEncodingException {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("layoutDefinition", getEncodedLayoutDefinition(previewLayoutDef));
        parameters.put("layoutMode", layoutMode);
        parameters.put("layoutTemplate", layoutTemplate);
        return URIUtils.addParametersToURIQuery(LayoutDemoManager.PREVIEW_PATH
                + LayoutDemoURLCodec.LAYOUT_PREVIEW_FRAME_VIEW_ID, parameters);
    }

    public LayoutDefinition getLayoutDefinition(PreviewLayoutDefinition previewLayoutDef) {
        if (previewLayoutDef == null) {
            return null;
        }
        WidgetDefinition widgetDef = new WidgetDefinitionImpl("preview_widget", previewLayoutDef.getWidgetType(),
                previewLayoutDef.getLabel(), previewLayoutDef.getHelpLabel(),
                Boolean.TRUE.equals(previewLayoutDef.getTranslated()), null, previewLayoutDef.getFieldDefinitions(),
                previewLayoutDef.getWidgetProperties(), previewLayoutDef.getSubWidgets());
        widgetDef.setHandlingLabels(Boolean.TRUE.equals(previewLayoutDef.getHandlingLabels()));
        return new LayoutDefinitionImpl("preview_layout", null, widgetDef);
    }

    public String getEncodedLayoutDefinition(PreviewLayoutDefinition previewLayoutDef)
            throws UnsupportedEncodingException {
        LayoutDefinition def = getLayoutDefinition(previewLayoutDef);
        return getEncodedLayoutDefinition(def);
    }

    public String getEncodedLayoutDefinition(LayoutDefinition def) throws UnsupportedEncodingException {
        JSONObject json = JSONLayoutExporter.exportToJson(LAYOUT_CATEGORY, def);
        if (log.isDebugEnabled()) {
            log.debug("Encoded layout definition: " + json.toString());
        }
        return JSONLayoutExporter.encode(json);
    }

    public LayoutDefinition getDecodedLayoutDefinition(String jsonEncodedLayoutDef) throws UnsupportedEncodingException {
        if (StringUtils.isBlank(jsonEncodedLayoutDef)) {
            return null;
        }
        JSONObject json = JSONLayoutExporter.decode(jsonEncodedLayoutDef);
        if (log.isDebugEnabled()) {
            log.debug("Decoded layout definition: " + json.toString());
        }
        return JSONLayoutExporter.importLayoutDefinition(json);
    }

}
