/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoWidgetType;
import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.runtime.api.Framework;

/**
 * Seam component providing a document model for layout demo and testing, and
 * handling reset of this document model when the page changes.
 *
 * @author Anahide Tchertchian
 */
@Name("layoutDemoActions")
@Scope(SESSION)
public class LayoutDemoActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected LayoutDemoManager layoutDemoManager;

    @In(create = true)
    protected WebLayoutManager webLayoutManager;

    @In(create = true)
    protected DocumentModel layoutBareDemoDocument;

    protected DocumentModel layoutDemoDocument;

    protected DemoWidgetType currentWidgetType;

    protected String currentTabId;

    protected String currentSubTabId;

    protected PreviewLayoutDefinition viewPreviewLayoutDef;

    protected PreviewLayoutDefinition editPreviewLayoutDef;

    @Factory(value = "layoutDemoDocument", scope = EVENT)
    public DocumentModel getDemoDocument() throws ClientException {
        if (layoutDemoDocument == null) {
            try {
                layoutDemoDocument = layoutBareDemoDocument.clone();
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return layoutDemoDocument;
    }

    public String initContextFromRestRequest(DocumentView docView)
            throws ClientException {

        DemoWidgetType widgetType = null;
        boolean isPreviewFrame = false;
        if (docView != null) {
            String viewId = docView.getViewId();
            if (viewId != null) {
                // try to deduce current widget type
                widgetType = layoutDemoManager.getWidgetTypeByViewId(viewId);
            }
        }

        if (!isPreviewFrame) {
            // avoid resetting contextual info when generating preview frame
            setCurrentWidgetType(widgetType);
        }

        return null;
    }

    public void setCurrentWidgetType(DemoWidgetType newWidgetType) {
        if (currentWidgetType != null
                && !currentWidgetType.equals(newWidgetType)) {
            // reset demo doc too
            layoutDemoDocument = null;
            viewPreviewLayoutDef = null;
            editPreviewLayoutDef = null;
            currentTabId = null;
            currentSubTabId = null;
        }
        currentWidgetType = newWidgetType;
    }

    @Factory(value = "currentWidgetType", scope = EVENT)
    public DemoWidgetType getCurrentWidgetType() {
        return currentWidgetType;
    }

    @Factory(value = "currentWidgetTypeDef", scope = EVENT)
    public WidgetTypeDefinition getCurrentWidgetTypeDefinition() {
        if (currentWidgetType != null) {
            String type = currentWidgetType.getName();
            LayoutStore lm = Framework.getLocalService(LayoutStore.class);
            return lm.getWidgetTypeDefinition(
                    currentWidgetType.getWidgetTypeCategory(), type);
        }
        return null;
    }

    @Factory(value = "layoutDemoCurrentTabId", scope = EVENT)
    public String getCurrentTabId() {
        return currentTabId;
    }

    public void setCurrentTabId(String currentTabId) {
        this.currentTabId = currentTabId;
    }

    @Factory(value = "layoutDemoCurrentSubTabId", scope = EVENT)
    public String getCurrentSubTabId() {
        return currentSubTabId;
    }

    public void setCurrentSubTabId(String currentSubTabId) {
        this.currentSubTabId = currentSubTabId;
    }

    protected PreviewLayoutDefinition createPreviewLayoutDefinition(
            DemoWidgetType widgetType) {
        String type = widgetType.getName();

        Map<String, Serializable> props = new HashMap<>();
        if (widgetType.getDefaultProperties() != null) {
            props.putAll(widgetType.getDefaultProperties());
        }
        if (type != null && type.contains("Aggregate")) {
            // fill up aggregates mapping as default properties
            if (type.contains("Aggregate")) {
                props.put("selectOptions",
                        "#{layoutDemoAggregates['dir_terms'].buckets}");
            } else {
                props.put("selectOptions",
                        "#{layoutDemoAggregates['string_terms'].buckets}");
            }
            props.put("directoryName", "layout_demo_crew");
        }
        PreviewLayoutDefinition def = new PreviewLayoutDefinition(
                widgetType.getName(), widgetType.getFields(), props);

        LayoutStore lm = Framework.getLocalService(LayoutStore.class);
        WidgetTypeDefinition wtDef = lm.getWidgetTypeDefinition(
                widgetType.getWidgetTypeCategory(), type);
        if (def != null) {
            WidgetTypeConfiguration wtConf = wtDef.getConfiguration();
            if (wtConf.isAcceptingSubWidgets()) {
                // add some subwidgets for preview needs, hardcoded right now
                if ("list".equals(type)) {
                    LayoutDefinition ldef = webLayoutManager.getLayoutDefinition("complexListWidgetLayout");
                    def.setSubWidgets(retrieveSubWidgets(ldef));
                } else if ("complex".equals(type)) {
                    LayoutDefinition ldef = webLayoutManager.getLayoutDefinition("complexWidgetLayout");
                    def.setSubWidgets(retrieveSubWidgets(ldef));
                } else if ("container".equals(type)) {
                    LayoutDefinition ldef = webLayoutManager.getLayoutDefinition("containerWidgetLayout");
                    def.setSubWidgets(retrieveSubWidgets(ldef));
                    def.setHandlingLabels(Boolean.TRUE);
                }
            }
            if ("actions".equals(type)) {
                def.setHandlingLabels(Boolean.TRUE);
            }
        }

        // set a custom label and help label
        def.setLabel("My widget label");
        def.setHelpLabel("My widget help label");
        return def;
    }

    protected List<WidgetDefinition> retrieveSubWidgets(
            LayoutDefinition layoutDef) {
        List<WidgetDefinition> res = new ArrayList<WidgetDefinition>();
        LayoutRowDefinition[] rows = layoutDef.getRows();
        if (rows != null && rows.length > 0) {
            WidgetReference[] refs = rows[0].getWidgetReferences();
            if (refs != null && refs.length > 0) {
                String wName = refs[0].getName();
                WidgetDefinition wDef = layoutDef.getWidgetDefinition(wName);
                if (wDef != null) {
                    WidgetDefinition[] subs = wDef.getSubWidgetDefinitions();
                    if (subs != null) {
                        res.addAll(Arrays.asList(subs));
                    }
                }
            }
        }
        return res;
    }

    @Factory(value = "viewPreviewLayoutDef", scope = EVENT)
    public PreviewLayoutDefinition getViewPreviewLayoutDefinition() {
        if (viewPreviewLayoutDef == null && currentWidgetType != null) {
            viewPreviewLayoutDef = createPreviewLayoutDefinition(currentWidgetType);
        }
        return viewPreviewLayoutDef;
    }

    @Factory(value = "editPreviewLayoutDef", scope = EVENT)
    public PreviewLayoutDefinition getEditPreviewLayoutDefinition() {
        if (editPreviewLayoutDef == null && currentWidgetType != null) {
            editPreviewLayoutDef = createPreviewLayoutDefinition(currentWidgetType);
        }
        return editPreviewLayoutDef;
    }

}
