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
import java.util.List;

import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoWidgetType;
import org.nuxeo.ecm.platform.forms.layout.demo.service.LayoutDemoManager;
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

    protected DocumentModel bareDemoDocument;

    protected DocumentModel demoDocument;

    protected DemoWidgetType currentWidgetType;

    @Factory(value = "layoutDemoDocument", scope = EVENT)
    public DocumentModel getDemoDocument() throws ClientException {
        if (demoDocument == null) {
            if (bareDemoDocument == null) {
                // retrieve type from schema service and initialize document
                try {
                    SchemaManager schemaManager = Framework.getService(SchemaManager.class);
                    DocumentType type = schemaManager.getDocumentType(DEMO_DOCUMENT_TYPE);
                    bareDemoDocument = DocumentModelFactory.createDocumentModel(type);
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

    @Factory(value = "builtinWidgetTypes", scope = SESSION)
    public List<DemoWidgetType> getBuiltinWidgetTypes() {
        return layoutDemoManager.getWidgetTypes("builtin");
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
        }
        currentWidgetType = newWidgetType;
    }

    @Factory(value = "currentWidgetType", scope = EVENT)
    public DemoWidgetType getCurrentWidgetType() {
        return currentWidgetType;
    }

}
