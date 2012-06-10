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
package org.nuxeo.ecm.platform.forms.layout.demo.jsf;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;
import org.nuxeo.ecm.platform.forms.layout.io.JSONLayoutExporter;

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

    public String getPreviewLayoutURL(PreviewLayoutDefinition previewLayoutDef,
            String layoutMode, String layoutTemplate)
            throws UnsupportedEncodingException, ClientException {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("layoutDefinition",
                getEncodedLayoutDefinition(previewLayoutDef));
        parameters.put("layoutMode", layoutMode);
        parameters.put("layoutTemplate", layoutTemplate);
        return URIUtils.addParametersToURIQuery(LayoutDemoManager.PREVIEW_PATH
                + LayoutDemoURLCodec.LAYOUT_PREVIEW_FRAME_VIEW_ID, parameters);
    }

    public LayoutDefinition getLayoutDefinition(
            PreviewLayoutDefinition previewLayoutDef) {
        if (previewLayoutDef == null) {
            return null;
        }
        WidgetDefinition widgetDef = new WidgetDefinitionImpl("preview_widget",
                previewLayoutDef.getWidgetType(), previewLayoutDef.getLabel(),
                previewLayoutDef.getHelpLabel(),
                Boolean.TRUE.equals(previewLayoutDef.getTranslated()), null,
                previewLayoutDef.getFieldDefinitions(),
                previewLayoutDef.getWidgetProperties(),
                previewLayoutDef.getSubWidgets());
        widgetDef.setHandlingLabels(Boolean.TRUE.equals(previewLayoutDef.getHandlingLabels()));
        return new LayoutDefinitionImpl("preview_layout", null, widgetDef);
    }

    public String getEncodedLayoutDefinition(
            PreviewLayoutDefinition previewLayoutDef)
            throws UnsupportedEncodingException {
        LayoutDefinition def = getLayoutDefinition(previewLayoutDef);
        return getEncodedLayoutDefinition(def);
    }

    public String getEncodedLayoutDefinition(LayoutDefinition def)
            throws UnsupportedEncodingException {
        JSONObject json = JSONLayoutExporter.exportToJson(LAYOUT_CATEGORY, def);
        if (log.isDebugEnabled()) {
            log.debug("Encoded layout definition: " + json.toString());
        }
        return JSONLayoutExporter.encode(json);
    }

    public LayoutDefinition getDecodedLayoutDefinition(
            String jsonEncodedLayoutDef) throws UnsupportedEncodingException,
            ClientException {
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
