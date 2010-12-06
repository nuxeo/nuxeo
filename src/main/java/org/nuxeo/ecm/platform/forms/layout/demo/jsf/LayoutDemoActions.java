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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
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

    public static final String DEMO_DOCUMENT_TYPE = "LayoutDemoDocument";

    @In(create = true)
    protected LayoutDemoManager layoutDemoManager;

    @In(create = true)
    protected WebLayoutManager webLayoutManager;

    protected DocumentModel bareDemoDocument;

    protected DocumentModel demoDocument;

    protected DemoWidgetType currentWidgetType;

    protected String currentTabId;

    protected String currentSubTabId;

    protected Boolean showViewPreview;

    protected PreviewLayoutDefinition viewPreviewLayoutDef;

    protected Boolean showEditPreview;

    protected PreviewLayoutDefinition editPreviewLayoutDef;

    @Factory(value = "layoutDemoDocument", scope = EVENT)
    public DocumentModel getDemoDocument() throws ClientException {
        if (demoDocument == null) {
            if (bareDemoDocument == null) {
                // retrieve type from schema service and initialize document
                try {
                    SchemaManager schemaManager = Framework.getService(SchemaManager.class);
                    DocumentType type = schemaManager.getDocumentType(DEMO_DOCUMENT_TYPE);
                    bareDemoDocument = DocumentModelFactory.createDocumentModel(
                            null, type);
                } catch (Exception e) {
                    throw new ClientException(e);
                }
            }
            try {
                demoDocument = bareDemoDocument.clone();
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return demoDocument;
    }

    @Factory(value = "standardWidgetTypes", scope = SESSION)
    public List<DemoWidgetType> getStandardWidgetTypes() {
        return layoutDemoManager.getWidgetTypes("standard");
    }

    @Factory(value = "customWidgetTypes", scope = SESSION)
    public List<DemoWidgetType> getCustomWidgetTypes() {
        return layoutDemoManager.getWidgetTypes("custom");
    }

    public String initContextFromRestRequest(DocumentView docView)
            throws ClientException {

        DemoWidgetType widgetType = null;
        if (docView != null) {
            String viewId = docView.getViewId();
            if (viewId != null) {
                widgetType = layoutDemoManager.getWidgetTypeByViewId(viewId);
            }
        }

        setCurrentWidgetType(widgetType);

        return null;
    }

    public void setCurrentWidgetType(DemoWidgetType newWidgetType) {
        if (currentWidgetType != null
                && !currentWidgetType.equals(newWidgetType)) {
            // reset demo doc too
            demoDocument = null;
            showViewPreview = null;
            viewPreviewLayoutDef = null;
            showEditPreview = null;
            editPreviewLayoutDef = null;
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
            return webLayoutManager.getWidgetTypeDefinition(type);
        }
        return null;
    }

    @Factory(value = "currentWidgetTypeConf", scope = EVENT)
    public WidgetTypeConfiguration getCurrentWidgetTypeConfiguration() {
        WidgetTypeDefinition def = getCurrentWidgetTypeDefinition();
        if (def != null) {
            return def.getConfiguration();
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

    public Boolean getShowViewPreview() {
        return showViewPreview;
    }

    public void setShowViewPreview(Boolean showViewPreview) {
        this.showViewPreview = showViewPreview;
    }

    @Factory(value = "viewPreviewLayoutDef", scope = EVENT)
    public PreviewLayoutDefinition getViewPreviewLayoutDefinition() {
        if (viewPreviewLayoutDef == null && currentWidgetType != null) {
            viewPreviewLayoutDef = new PreviewLayoutDefinition(
                    currentWidgetType.getName(), currentWidgetType.getFields());
        }
        return viewPreviewLayoutDef;
    }

    @Factory(value = "editPreviewLayoutDef", scope = EVENT)
    public PreviewLayoutDefinition getEditPreviewLayoutDefinition() {
        if (editPreviewLayoutDef == null && currentWidgetType != null) {
            editPreviewLayoutDef = new PreviewLayoutDefinition(
                    currentWidgetType.getName(), currentWidgetType.getFields());
        }
        return editPreviewLayoutDef;
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
                cleanUpProperties(previewLayoutDef.getProperties()), null);
        return new LayoutDefinitionImpl("preview_layout", null, widgetDef);
    }

    /**
     * Removes empty properties as the JSF component may not accept empty
     * values for some properties like "converter" or "validator".
     */
    protected Map<String, Serializable> cleanUpProperties(
            Map<String, Serializable> props) {
        Map<String, Serializable> res = new HashMap<String, Serializable>();
        if (props != null) {
            for (Map.Entry<String, Serializable> prop : props.entrySet()) {
                Serializable value = prop.getValue();
                if (value == null
                        || (value instanceof String && StringUtils.isEmpty((String) value))) {
                    continue;
                }
                res.put(prop.getKey(), value);
            }
        }
        return res;
    }

    public Boolean getShowEditPreview() {
        return showEditPreview;
    }

    public void setShowEditPreview(Boolean showEditPreview) {
        this.showEditPreview = showEditPreview;
    }

}
