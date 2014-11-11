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
 * $Id: WebLayoutManager.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.service;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutManager;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTypeHandler;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.TagConfig;

/**
 * Web Layout manager interface.
 * <p>
 * It manages registries of layout definitions and widget types and handles the
 * creation of layouts and widgets instances.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface WebLayoutManager extends LayoutManager {

    /**
     * Returns the widget type handler for the registered widget type with this
     * type name.
     * <p>
     * If the no widget type is found with this name, return null.
     */
    WidgetTypeHandler getWidgetTypeHandler(String typeName)
            throws WidgetException;

    /**
     * Returns the computed layout for this name and mode in given context.
     * <p>
     * When a widget is configured not to be rendered in this mode, the layout
     * will hold a null value instead. As well, when a row does not hold any
     * non-null widget in this mode, the layout will not hold it.
     *
     * @param ctx the facelet context this layout will be computed in. If
     *            context is null, no expressions can be resolved during
     *            computing.
     * @param layoutName the layout definition name.
     * @param mode the mode.
     * @param valueName the value name to use when computing tag attributes.
     * @return a layout computed in this context.
     */
    Layout getLayout(FaceletContext ctx, String layoutName, String mode,
            String valueName);

    /**
     * Returns the facelet handler for given widget.
     *
     * @param ctx the facelet context.
     * @param config the tag config, used to hook the handler in the jsf tree.
     * @param widget the computed widget.
     * @return a facelet handler.
     */
    FaceletHandler getFaceletHandler(FaceletContext ctx, TagConfig config,
            Widget widget);

    /**
     * Returns a widget computed from given information.
     *
     * @param ctx the facelet context this layout will be computed in. If
     *            context is null, no expressions can be resolved during
     *            computing.
     * @param type the widget type name.
     * @param mode the mode.
     * @param valueName the value name to use when computing tag attributes.
     * @param properties optional properties to use when computing the widget.
     * @param subWidgets optional sub widgets for this widget.
     * @return a widget computed in this context.
     */
    Widget createWidget(FaceletContext ctx, String type, String mode,
            String valueName, Map<String, Serializable> properties,
            Widget[] subWidgets);

}
