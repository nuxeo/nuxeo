/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LayoutTagLibrary.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets.library;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.functions.LayoutFunctions;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.runtime.api.Framework;

/**
 * Layout tag library.
 * <p>
 * Since 7.4, only holds JSF functions for the nxl tag library.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutTagLibrary {

    private static final Log log = LogFactory.getLog(LayoutTagLibrary.class);

    // JSF functions

    public static WidgetTypeDefinition getWidgetTypeDefinition(String typeName) {
        WebLayoutManager layoutService = Framework.getService(WebLayoutManager.class);
        return layoutService.getWidgetTypeDefinition(typeName);
    }

    /**
     * Returns a String representing each of the field definitions property name, separated by a space.
     */
    public static String getFieldDefinitionsAsString(FieldDefinition[] defs) {
        return LayoutFunctions.getFieldDefinitionsAsString(defs);
    }

    public static List<LayoutRow> getSelectedRows(Layout layout, List<String> selectedRowNames,
            boolean showAlwaysSelected) {
        return LayoutFunctions.getSelectedRows(layout, selectedRowNames, showAlwaysSelected);
    }

    public static List<LayoutRow> getNotSelectedRows(Layout layout, List<String> selectedRowNames) {
        return LayoutFunctions.getNotSelectedRows(layout, selectedRowNames);
    }

    public static List<String> getDefaultSelectedRowNames(Layout layout, boolean showAlwaysSelected) {
        return LayoutFunctions.getDefaultSelectedRowNames(layout, showAlwaysSelected);
    }

    /**
     * Joins two strings to get a valid reRender attribute for ajax components.
     *
     * @since 5.7
     * @deprecated since 6.0: use {@link #joinRender(String, String)} instead.
     */
    @Deprecated
    public static String joinReRender(String render1, String render2) {
        log.warn("Method nxl:joinReRender is deprecated, use nxu:joinRender instead");
        return Functions.joinRender(render1, render2);
    }

}
