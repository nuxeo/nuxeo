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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LayoutTagLibrary.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.library;

import java.lang.reflect.Method;
import java.util.List;

import javax.faces.FacesException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.facelets.DocumentLayoutTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.LayoutRowTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.LayoutRowWidgetTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.LayoutTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.SubWidgetTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTypeTagHandler;
import org.nuxeo.ecm.platform.forms.layout.functions.LayoutFunctions;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.facelets.tag.AbstractTagLibrary;

/**
 * Layout tag library
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutTagLibrary extends AbstractTagLibrary {

    private static final Log log = LogFactory.getLog(LayoutTagLibrary.class);

    public static final String Namespace = "http://nuxeo.org/nxforms/layout";

    public static final LayoutTagLibrary Instance = new LayoutTagLibrary();

    public LayoutTagLibrary() {
        super(Namespace);
        addTagHandler("widgetType", WidgetTypeTagHandler.class);
        addTagHandler("widget", WidgetTagHandler.class);
        addTagHandler("layout", LayoutTagHandler.class);
        addTagHandler("layoutRow", LayoutRowTagHandler.class);
        addTagHandler("layoutColumn", LayoutRowTagHandler.class);
        addTagHandler("layoutRowWidget", LayoutRowWidgetTagHandler.class);
        addTagHandler("layoutColumnWidget", LayoutRowWidgetTagHandler.class);
        addTagHandler("subWidget", SubWidgetTagHandler.class);
        addTagHandler("documentLayout", DocumentLayoutTagHandler.class);

        try {
            addFunction("widgetTypeDefinition",
                    LayoutTagLibrary.class.getMethod("getWidgetTypeDefinition",
                            new Class[] { String.class }));
        } catch (NoSuchMethodException e) {
            log.error(e, e);
        }

        try {
            addFunction("fieldDefinitionsAsString",
                    LayoutTagLibrary.class.getMethod(
                            "getFieldDefinitionsAsString",
                            new Class[] { FieldDefinition[].class }));
        } catch (NoSuchMethodException e) {
            log.error(e, e);
        }

        try {
            Method getSelectedRows = LayoutTagLibrary.class.getMethod(
                    "getSelectedRows", new Class[] { Layout.class, List.class,
                            boolean.class });
            addFunction("selectedRows", getSelectedRows);
            addFunction("selectedColumns", getSelectedRows);
        } catch (NoSuchMethodException e) {
            log.error(e, e);
        }

        try {
            Method getNotSelectedRows = LayoutTagLibrary.class.getMethod(
                    "getNotSelectedRows", new Class[] { Layout.class,
                            List.class });
            addFunction("notSelectedRows", getNotSelectedRows);
            addFunction("notSelectedColumns", getNotSelectedRows);
        } catch (NoSuchMethodException e) {
            log.error(e, e);
        }

        try {
            Method getDefaultSelectedRowNames = LayoutTagLibrary.class.getMethod(
                    "getDefaultSelectedRowNames", new Class[] { Layout.class,
                            boolean.class });
            addFunction("defaultSelectedRowNames", getDefaultSelectedRowNames);
            addFunction("defaultSelectedColumnNames",
                    getDefaultSelectedRowNames);
        } catch (NoSuchMethodException e) {
            log.error(e, e);
        }

        try {
            Method isBoundToEditMode = BuiltinModes.class.getMethod(
                    "isBoundToEditMode", new Class[] { String.class });
            addFunction("isBoundToEditMode", isBoundToEditMode);
        } catch (NoSuchMethodException e) {
            log.error(e, e);
        }

        try {
            Method isLikePlainMode = BuiltinWidgetModes.class.getMethod(
                    "isLikePlainMode", new Class[] { String.class });
            addFunction("isLikePlainMode", isLikePlainMode);
        } catch (NoSuchMethodException e) {
            log.error(e, e);
        }

        try {
            Method isLikeViewMode = BuiltinWidgetModes.class.getMethod(
                    "isLikeViewMode", new Class[] { String.class });
            addFunction("isLikeViewMode", isLikeViewMode);
        } catch (NoSuchMethodException e) {
            log.error(e, e);
        }

        // BBB
        try {
            Method joinReRender = LayoutTagLibrary.class.getMethod(
                    "joinReRender", new Class[] { String.class, String.class });
            addFunction("joinReRender", joinReRender);
        } catch (NoSuchMethodException e) {
            log.error(e, e);
        }

    }

    // JSF functions

    public static WidgetTypeDefinition getWidgetTypeDefinition(String typeName) {
        WebLayoutManager layoutService;
        try {
            layoutService = Framework.getService(WebLayoutManager.class);
        } catch (Exception e) {
            throw new FacesException(e);
        }
        if (layoutService == null) {
            throw new FacesException("Layout service not found");
        }
        return layoutService.getWidgetTypeDefinition(typeName);
    }

    /**
     * Returns a String representing each of the field definitions property
     * name, separated by a space.
     */
    public static String getFieldDefinitionsAsString(FieldDefinition[] defs) {
        return LayoutFunctions.getFieldDefinitionsAsString(defs);
    }

    public static List<LayoutRow> getSelectedRows(Layout layout,
            List<String> selectedRowNames, boolean showAlwaysSelected) {
        return LayoutFunctions.getSelectedRows(layout, selectedRowNames,
                showAlwaysSelected);
    }

    public static List<LayoutRow> getNotSelectedRows(Layout layout,
            List<String> selectedRowNames) {
        return LayoutFunctions.getNotSelectedRows(layout, selectedRowNames);
    }

    public static List<String> getDefaultSelectedRowNames(Layout layout,
            boolean showAlwaysSelected) {
        return LayoutFunctions.getDefaultSelectedRowNames(layout,
                showAlwaysSelected);
    }

    /**
     * Joins two strings to get a valid reRender attribute for ajax components.
     *
     * @since 5.7
     * @deprecated since 5.9.4-JSF2: use {@link #joinRender(String, String)}
     *             instead.
     */
    @Deprecated
    public static String joinReRender(String render1, String render2) {
        log.warn("Method nxl:joinReRender is deprecated, use nxu:joinRender instead");
        return Functions.joinRender(render1, render2);
    }

}
