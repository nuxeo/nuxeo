/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.forms.layout.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;

import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutManager;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTypeHandler;

/**
 * Web Layout manager interface.
 * <p>
 * It manages registries of layout definitions and widget types and handles the creation of layouts and widgets
 * instances.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface WebLayoutManager extends LayoutManager {

    String JSF_CATEGORY = "jsf";

    /**
     * Returns the widget type handler for the registered widget type with this type name and type category.
     * <p>
     * If no widget type is found with this name, returns null.
     *
     * @since 8.1
     */
    WidgetTypeHandler getWidgetTypeHandler(TagConfig config, String typeCategory, String typeName)
            throws WidgetException;

    /**
     * Returns the widget type handler for the registered widget.
     * <p>
     * If widget is null or its widget type is unknown, returns null.
     *
     * @since 8.1
     */
    WidgetTypeHandler getWidgetTypeHandler(TagConfig config, Widget widget) throws WidgetException;

    /**
     * Returns the computed layout for this name and mode in given context, or null if no layout with this name is
     * found.
     * <p>
     * When a widget is configured not to be rendered in this mode, the layout will hold a null value instead. As well,
     * when a row does not hold any non-null widget in this mode, the layout will not hold it.
     *
     * @see #getLayout(FaceletContext, String, String, String, List, boolean)
     * @param ctx the facelet context this layout will be computed in. If context is null, no expressions can be
     *            resolved during computing.
     * @param layoutName the layout definition name.
     * @param mode the mode.
     * @param valueName the value name to use when computing tag attributes.
     * @return a layout computed in this context.
     */
    Layout getLayout(FaceletContext ctx, String layoutName, String mode, String valueName);

    /**
     * Returns the computed layout for this name, mode and list of selected rows in given context, or null if no layout
     * with this name is found.
     *
     * @see LayoutManager#getLayoutDefinition(String)
     * @see #getLayout(FaceletContext, LayoutDefinition, String, String, List, boolean)
     * @param layoutName the layout definition name.
     * @return a layout computed in this context.
     * @since 5.4
     */
    Layout getLayout(FaceletContext ctx, String layoutName, String mode, String valueName, List<String> selectedRows,
            boolean selectAllRowsByDefault);

    /**
     * Returns the computed layout for this name, category, mode and list of selected rows in given context, or null if
     * no layout with this name is found.
     *
     * @see LayoutManager#getLayoutDefinition(String)
     * @see #getLayout(FaceletContext, LayoutDefinition, String, String, List, boolean)
     * @param layoutName the layout definition name.
     * @return a layout computed in this context.
     * @since 5.5
     */
    Layout getLayout(FaceletContext ctx, String layoutName, String layoutCategory, String mode, String valueName,
            List<String> selectedRows, boolean selectAllRowsByDefault);

    /**
     * Returns the computed layout for this definition, mode and list of selected rows in given context, or null if the
     * layout definition is null.
     * <p>
     * When a widget is configured not to be rendered in this mode, the layout will hold a null value instead. As well,
     * when a row does not hold any non-null widget in this mode, the layout will not hold it.
     * <p>
     * If parameter selectedRows is not null, layout rows will be filtered according to this value. If selectedRows is
     * null and parameter selectAllRowsByDefault is true, all rows will be taken into account, even rows marked as not
     * selected by default.
     *
     * @param ctx the facelet context this layout will be computed in. If context is null, no expressions can be
     *            resolved during computing.
     * @param layoutDef the layout definition instance.
     * @param mode the mode.
     * @param valueName the value name to use when computing tag attributes (useful for fields mapping)
     * @param selectedRows the list of selected rows names
     * @param selectAllRowsByDefault boolean indicating if all rows should be considered selected by default in case
     *            parameter selectedRows resolves to null.
     * @return a layout computed in this context, null if definition is null.
     * @since 5.4
     */
    Layout getLayout(FaceletContext ctx, LayoutDefinition layoutDef, String mode, String valueName,
            List<String> selectedRows, boolean selectAllRowsByDefault);

    /**
     * Returns a layout with conversion.
     *
     * @since 7.3
     */
    Layout getLayout(FaceletContext ctx, LayoutConversionContext lctx, String conversionCat, LayoutDefinition layoutDef,
            String mode, String valueName, List<String> selectedRows, boolean selectAllRowsByDefault);

    /**
     * Returns a widget instance given a name and a category, as it would be computed when defined within a layout.
     *
     * @since 5.6
     * @param ctx the facelet context this widget will be computed in. If context is null, no expressions can be
     *            resolved during computing.
     * @param widgetName the widget name
     * @param widgetCategory the widget category
     * @param layoutMode the pseudo layout mode
     * @param valueName the value name to use when computing tag attributes (useful for fields mapping)
     * @param layoutName the pseudo layout name (if any)
     * @return the widget instance, or null if widget definition could not be resolved.
     */
    Widget getWidget(FaceletContext ctx, String widgetName, String widgetCategory, String layoutMode, String valueName,
            String layoutName);

    /**
     * Returns a widget instance given a name and a category, as it would be computed when defined within a layout.
     *
     * @since 5.6
     * @param ctx the facelet context this widget will be computed in. If context is null, no expressions can be
     *            resolved during computing.
     * @param widgetDef the widget definition
     * @param layoutMode the pseudo layout mode
     * @param valueName the value name to use when computing tag attributes (useful for fields mapping)
     * @param layoutName the pseudo layout name (if any)
     * @return the widget instance, or null if the widget definition is null.
     */
    Widget getWidget(FaceletContext ctx, WidgetDefinition widgetDef, String layoutMode, String valueName,
            String layoutName);

    /**
     * Returns a widget with conversion.
     *
     * @since 7.3
     */
    Widget getWidget(FaceletContext ctx, LayoutConversionContext lctx, String conversionCat, WidgetDefinition widgetDef,
            String layoutMode, String valueName, String layoutName);

    /**
     * Returns a widget computed from given information.
     *
     * @param ctx the facelet context this layout will be computed in. If context is null, no expressions can be
     *            resolved during computing.
     * @param type the widget type name.
     * @param mode the mode.
     * @param valueName the value name to use when computing tag attributes.
     * @param fieldDefinitions the field definitions
     * @param label the widget label
     * @param helpLabel the widget help label
     * @param translated if true, the labels will be translated
     * @param properties optional properties to use when computing the widget.
     * @param subWidgets optional sub widgets for this widget.
     * @return a widget computed in this context.
     * @since 5.4
     */
    Widget createWidget(FaceletContext ctx, String type, String mode, String valueName,
            List<FieldDefinition> fieldDefinitions, String label, String helpLabel, Boolean translated,
            Map<String, Serializable> properties, Widget[] subWidgets);

    /**
     * Returns a widget computed from given information.
     *
     * @param ctx the facelet context this layout will be computed in. If context is null, no expressions can be
     *            resolved during computing.
     * @param widgetDef the widget definition.
     * @param mode the mode.
     * @param valueName the value name to use when computing tag attributes.
     * @param subWidgets optional sub widgets for this widget.
     * @return a widget computed in this context.
     * @since 5.7.3
     */
    Widget createWidget(FaceletContext ctx, WidgetDefinition widgetDef, String mode, String valueName,
            Widget[] subWidgets);

    /**
     * Returns true if property with given name and value should be referenced as a value expression.
     * <p>
     * Referencing properties as value expressions makes it possible to resolve this value again when reloading
     * components in ajax for instance, as literal values kept by JSF components are not evaluated again.
     * <p>
     * But some components wait for a literal value and do not evaluate value expressions, so their properties should
     * not be referenced as value expressions. An extension point on the service makes it possible to declare these
     * properties: by default other properties will be using references.
     * <p>
     * This method returns false if it finds a matching disabled property ref for given criteria. If any of the given
     * parameters are null, this criterion is ignored, and this looks up any matching (and enabled) contribution.
     *
     * @since 5.7.3
     * @param name the property name
     * @param value the property value
     * @param widgetType the widget type if any
     * @param widgetTypeCategory the widget type category if any
     * @param mode the widget mode if any
     * @param template the widget template if any
     */
    boolean referencePropertyAsExpression(String name, Serializable value, String widgetType, String widgetTypeCategory,
            String mode, String template);

}
