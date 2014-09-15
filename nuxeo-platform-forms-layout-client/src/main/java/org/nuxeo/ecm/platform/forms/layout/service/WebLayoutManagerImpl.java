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
 * $Id: WebLayoutManagerImpl.java 28510 2008-01-06 10:21:44Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.view.facelets.ComponentHandler;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagConfig;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinWidgetModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.LayoutException;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutRowComparator;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutRowImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetImpl;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetReferenceImpl;
import org.nuxeo.ecm.platform.forms.layout.core.service.AbstractLayoutManager;
import org.nuxeo.ecm.platform.forms.layout.core.service.LayoutStoreImpl;
import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetTypeDescriptor;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.RenderVariables;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTypeHandler;
import org.nuxeo.ecm.platform.forms.layout.functions.LayoutFunctions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;

import com.sun.faces.facelets.el.VariableMapperWrapper;

/**
 * Layout service implementation.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class WebLayoutManagerImpl extends AbstractLayoutManager implements
        WebLayoutManager {

    public static final ComponentName NAME = new ComponentName(
            WebLayoutManagerImpl.class.getName());

    private static final Log log = LogFactory.getLog(WebLayoutManagerImpl.class);

    private static final long serialVersionUID = 1L;

    public static final String WIDGET_TYPES_EP_NAME = LayoutStoreImpl.WIDGET_TYPES_EP_NAME;

    public static final String WIDGETS_EP_NAME = LayoutStoreImpl.WIDGETS_EP_NAME;

    public static final String LAYOUTS_EP_NAME = LayoutStoreImpl.LAYOUTS_EP_NAME;

    public static final String PROPS_REF_EP_NAME = "disabledPropertyRefs";

    protected DisabledPropertyRefRegistry disabledPropertyRefsReg;

    // Runtime component API

    public WebLayoutManagerImpl() {
        super();
        disabledPropertyRefsReg = new DisabledPropertyRefRegistry();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            registerWidgetType(((WidgetTypeDescriptor) contribution).getWidgetTypeDefinition());
        } else if (extensionPoint.equals(LAYOUTS_EP_NAME)) {
            registerLayout(((LayoutDescriptor) contribution).getLayoutDefinition());
        } else if (extensionPoint.equals(WIDGETS_EP_NAME)) {
            registerWidget(((WidgetDescriptor) contribution).getWidgetDefinition());
        } else if (extensionPoint.equals(PROPS_REF_EP_NAME)) {
            registerDisabledPropertyRef(((DisabledPropertyRefDescriptor) contribution));
        } else {
            log.error(String.format(
                    "Unknown extension point '%s', can't register !",
                    extensionPoint));
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            unregisterWidgetType(((WidgetTypeDescriptor) contribution).getWidgetTypeDefinition());
        } else if (extensionPoint.equals(LAYOUTS_EP_NAME)) {
            unregisterLayout(((LayoutDescriptor) contribution).getLayoutDefinition());
        } else if (extensionPoint.equals(WIDGETS_EP_NAME)) {
            unregisterWidget(((WidgetDescriptor) contribution).getWidgetDefinition());
        } else if (extensionPoint.equals(PROPS_REF_EP_NAME)) {
            unregisterDisabledPropertyRef(((DisabledPropertyRefDescriptor) contribution));
        } else {
            log.error(String.format(
                    "Unknown extension point '%s', can't unregister !",
                    extensionPoint));
        }
    }

    // specific API (depends on JSF impl)

    @Override
    public String getDefaultStoreCategory() {
        return JSF_CATEGORY;
    }

    protected String getStoreCategory(String cat) {
        if (StringUtils.isBlank(cat)) {
            return getDefaultStoreCategory();
        }
        return cat;
    }

    @Override
    public WidgetTypeHandler getWidgetTypeHandler(String typeName)
            throws WidgetException {
        return getWidgetTypeHandler(getDefaultStoreCategory(), typeName);
    }

    @Override
    public WidgetTypeHandler getWidgetTypeHandler(String typeCategory,
            String typeName) throws WidgetException {
        if (StringUtils.isBlank(typeCategory)) {
            typeCategory = getDefaultStoreCategory();
        }
        WidgetType type = getLayoutStore().getWidgetType(typeCategory, typeName);
        if (type == null) {
            return null;
        }
        WidgetTypeHandler handler;
        try {
            // Thread context loader is not working in isolated EARs
            handler = (WidgetTypeHandler) type.getWidgetTypeClass().newInstance();
        } catch (Exception e) {
            log.error("Caught error when instanciating widget type handler", e);
            return null;
        }
        // set properties
        handler.setProperties(type.getProperties());
        return handler;
    }

    /**
     * Evaluates an EL expression in given context.
     * <p>
     * If the expression resolves to an EL expression, evaluate it again this
     * is useful when retrieving the expression from a configuration file.
     * <p>
     * If given context is null, do no try to evaluate it and return the
     * expression itself.
     *
     * @param context the facelet context.
     * @param expression the string expression.
     */
    protected static Object evaluateExpression(FaceletContext context,
            String expression) {
        if (expression == null) {
            return null;
        }
        if (context == null) {
            return expression;
        }
        Object value = ComponentTagUtils.resolveElExpression(context,
                expression);
        if (value != null && value instanceof String) {
            // evaluate a second time in case it's another EL expression
            value = ComponentTagUtils.resolveElExpression(context,
                    (String) value);
        }
        return value;
    }

    /**
     * Evaluates an expression to a boolean value.
     */
    protected static Boolean getBooleanValue(FaceletContext context,
            String expression) {
        Object value = evaluateExpression(context, expression);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value == null || value instanceof String) {
            return Boolean.valueOf((String) value);
        } else {
            log.error(String.format("Could not get boolean value for '%s'",
                    value));
            return Boolean.FALSE;
        }
    }

    /**
     * Evaluates an expression to a string value.
     */
    protected static String getStringValue(FaceletContext context,
            String expression) {
        Object value = evaluateExpression(context, expression);
        if (value == null || value instanceof String) {
            return (String) value;
        } else {
            log.error(String.format("Could not get string value for '%s'",
                    value));
            return null;
        }
    }

    protected static String getModeFromLayoutMode(FaceletContext context,
            WidgetDefinition wDef, String layoutMode) {
        String wMode = getStringValue(context, wDef.getMode(layoutMode));
        if (wMode == null) {
            wMode = BuiltinModes.getWidgetModeFromLayoutMode(layoutMode);
        }
        return wMode;
    }

    @Override
    public Widget getWidget(FaceletContext ctx, String widgetName,
            String widgetCategory, String layoutMode, String valueName,
            String layoutName) {
        WidgetReference widgetRef = new WidgetReferenceImpl(widgetCategory,
                widgetName);
        WidgetDefinition wDef = lookupWidget(widgetRef);
        if (wDef != null) {
            return getWidget(ctx, layoutName, null, wDef, layoutMode,
                    valueName, 0);
        }
        return null;
    }

    @Override
    public Widget getWidget(FaceletContext ctx, WidgetDefinition wDef,
            String layoutMode, String valueName, String layoutName) {
        if (wDef != null) {
            return getWidget(ctx, layoutName, null, wDef, layoutMode,
                    valueName, 0);
        }
        return null;
    }

    /**
     * Computes a widget from a definition for a mode in a given context.
     * <p>
     * If the widget is configured not to be rendered in the given mode,
     * returns null.
     * <p>
     * Sub widgets are also computed recursively.
     */
    @SuppressWarnings("deprecation")
    protected Widget getWidget(FaceletContext context, String layoutName,
            LayoutDefinition layoutDef, WidgetDefinition wDef,
            String layoutMode, String valueName, int level) {
        VariableMapper orig = null;
        // avoid variable mapper changes if context is null for tests
        if (context != null) {
            // expose widget mode so that it can be used in a mode el
            // expression
            orig = context.getVariableMapper();
            VariableMapper vm = new VariableMapperWrapper(orig);
            context.setVariableMapper(vm);
            ExpressionFactory eFactory = context.getExpressionFactory();
            ValueExpression modeVe = eFactory.createValueExpression(layoutMode,
                    String.class);
            vm.setVariable(RenderVariables.globalVariables.mode.name(), modeVe);
        }
        String wMode = getModeFromLayoutMode(context, wDef, layoutMode);
        if (context != null) {
            context.setVariableMapper(orig);
        }

        if (BuiltinWidgetModes.HIDDEN.equals(wMode)) {
            return null;
        }
        List<Widget> subWidgets = new ArrayList<Widget>();
        WidgetDefinition[] swDefs = wDef.getSubWidgetDefinitions();
        if (swDefs != null) {
            for (WidgetDefinition swDef : swDefs) {
                Widget subWidget = getWidget(context, layoutName, layoutDef,
                        swDef, wMode, valueName, level + 1);
                if (subWidget != null) {
                    subWidgets.add(subWidget);
                }
            }
        }

        WidgetReference[] swRefs = wDef.getSubWidgetReferences();
        if (swRefs != null) {
            for (WidgetReference swRef : swRefs) {
                WidgetDefinition swDef = lookupWidget(layoutDef, swRef);
                if (swDef == null) {
                    log.error(String.format(
                            "Widget '%s' not found in layout %s",
                            swRef.getName(), layoutName));
                } else {
                    Widget subWidget = getWidget(context, layoutName,
                            layoutDef, swDef, wMode, valueName, level + 1);

                    if (subWidget != null) {
                        subWidgets.add(subWidget);
                    }
                }
            }
        }

        boolean required = getBooleanValue(context,
                wDef.getRequired(layoutMode, wMode)).booleanValue();

        String wType = wDef.getType();
        String wTypeCat = wDef.getTypeCategory();
        // fill default property values from the widget definition
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        WidgetTypeDefinition def = getLayoutStore().getWidgetTypeDefinition(
                getStoreCategory(wTypeCat), wType);

        WidgetTypeConfiguration conf = def != null ? def.getConfiguration()
                : null;
        if (conf != null) {
            Map<String, Serializable> defaultProps = conf.getDefaultPropertyValues(wMode);
            if (defaultProps != null && !defaultProps.isEmpty()) {
                props.putAll(defaultProps);
            }
        }

        props.putAll(wDef.getProperties(layoutMode, wMode));

        WidgetImpl widget = new WidgetImpl(layoutName, wDef.getName(), wMode,
                wType, valueName, wDef.getFieldDefinitions(),
                wDef.getLabel(layoutMode), wDef.getHelpLabel(layoutMode),
                wDef.isTranslated(), wDef.isHandlingLabels(), props, required,
                subWidgets.toArray(new Widget[0]), level,
                wDef.getSelectOptions(),
                LayoutFunctions.computeWidgetDefinitionId(wDef),
                wDef.getRenderingInfos(layoutMode));
        Map<String, Serializable> controls = wDef.getControls(layoutMode, wMode);
        if (controls != null) {
            for (Map.Entry<String, Serializable> control : controls.entrySet()) {
                widget.setControl(control.getKey(), control.getValue());
            }
        }
        widget.setTypeCategory(getStoreCategory(wTypeCat));
        return widget;
    }

    @Override
    public Layout getLayout(FaceletContext ctx, String layoutName, String mode,
            String valueName) throws LayoutException {
        return getLayout(ctx, layoutName, mode, valueName, null, false);
    }

    @Override
    public Layout getLayout(FaceletContext ctx, String layoutName, String mode,
            String valueName, List<String> selectedRows,
            boolean selectAllRowsByDefault) {
        return getLayout(ctx, layoutName, null, mode, valueName, selectedRows,
                selectAllRowsByDefault);
    }

    @Override
    public Layout getLayout(FaceletContext ctx, String layoutName,
            String layoutCategory, String mode, String valueName,
            List<String> selectedRows, boolean selectAllRowsByDefault) {
        if (StringUtils.isBlank(layoutCategory)) {
            layoutCategory = getDefaultStoreCategory();
        }
        LayoutDefinition layoutDef = getLayoutStore().getLayoutDefinition(
                layoutCategory, layoutName);
        if (layoutDef == null) {
            log.debug(String.format("Layout '%s' not found for category '%s'",
                    layoutName, layoutCategory));
            return null;
        }
        return getLayout(ctx, layoutDef, mode, valueName, selectedRows,
                selectAllRowsByDefault);
    }

    @Override
    public Layout getLayout(FaceletContext ctx, LayoutDefinition layoutDef,
            String mode, String valueName, List<String> selectedRows,
            boolean selectAllRowsByDefault) {
        if (layoutDef == null) {
            log.debug("Layout definition is null");
            return null;
        }
        String layoutName = layoutDef.getName();
        if (ctx == null) {
            log.warn("Layout creation computed in a null facelet context: expressions "
                    + "found in the layout definition will not be evaluated");
        }
        LayoutRowDefinition[] rowsDef = layoutDef.getRows();
        List<LayoutRow> rows = new ArrayList<LayoutRow>();
        Set<String> foundRowNames = new HashSet<String>();
        int rowIndex = -1;
        for (LayoutRowDefinition rowDef : rowsDef) {
            rowIndex++;
            String rowName = rowDef.getName();
            if (rowName == null) {
                rowName = "layout_row_" + rowIndex;
                if (selectedRows != null) {
                    log.debug(String.format("Generating default name '%s' in "
                            + "layout '%s' for row or column at index %s",
                            rowName, layoutName, Integer.valueOf(rowIndex)));
                }
            }
            boolean emptyRow = true;
            if (selectedRows != null && !selectedRows.contains(rowName)
                    && !rowDef.isAlwaysSelected()) {
                continue;
            }
            if (selectedRows == null && !selectAllRowsByDefault
                    && !rowDef.isSelectedByDefault()
                    && !rowDef.isAlwaysSelected()) {
                continue;
            }
            List<Widget> widgets = new ArrayList<Widget>();
            for (WidgetReference widgetRef : rowDef.getWidgetReferences()) {
                String widgetName = widgetRef.getName();
                if (widgetName == null || widgetName.length() == 0) {
                    // no widget at this place
                    widgets.add(null);
                    continue;
                }
                WidgetDefinition wDef = lookupWidget(layoutDef, widgetRef);
                if (wDef == null) {
                    log.error(String.format(
                            "Widget '%s' not found in layout %s", widgetName,
                            layoutName));
                    widgets.add(null);
                    continue;
                }
                Widget widget = getWidget(ctx, layoutName, layoutDef, wDef,
                        mode, valueName, 0);
                if (widget != null) {
                    emptyRow = false;
                }
                widgets.add(widget);
            }
            if (!emptyRow) {
                rows.add(new LayoutRowImpl(rowName,
                        rowDef.isSelectedByDefault(),
                        rowDef.isAlwaysSelected(), widgets,
                        rowDef.getProperties(mode),
                        LayoutFunctions.computeLayoutRowDefinitionId(rowDef)));
            }
            foundRowNames.add(rowName);
        }
        if (selectedRows != null) {
            Collections.sort(rows, new LayoutRowComparator(selectedRows));
            for (String selectedRow : selectedRows) {
                if (!foundRowNames.contains(selectedRow)) {
                    log.debug(String.format(
                            "Selected row or column named '%s' "
                                    + "was not found in layout '%s'",
                            selectedRow, layoutName));
                }
            }
        }
        int columns = layoutDef.getColumns();
        Layout layout = new LayoutImpl(layoutDef.getName(), mode,
                layoutDef.getTemplate(mode), rows, columns,
                layoutDef.getProperties(mode),
                LayoutFunctions.computeLayoutDefinitionId(layoutDef));
        layout.setValueName(valueName);
        return layout;
    }

    protected WidgetDefinition lookupWidget(LayoutDefinition layoutDef,
            WidgetReference widgetRef) {
        String widgetName = widgetRef.getName();
        WidgetDefinition wDef = null;
        if (layoutDef != null) {
            wDef = layoutDef.getWidgetDefinition(widgetName);
        }
        if (wDef == null) {
            // try in global registry
            wDef = lookupWidget(widgetRef);
        }
        return wDef;
    }

    protected WidgetDefinition lookupWidget(WidgetReference widgetRef) {
        String widgetName = widgetRef.getName();
        String cat = widgetRef.getCategory();
        WidgetDefinition wDef;
        if (StringUtils.isBlank(cat)) {
            wDef = getWidgetDefinition(widgetName);
        } else {
            wDef = getLayoutStore().getWidgetDefinition(cat, widgetName);
        }
        return wDef;
    }

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig config, Widget widget) {
        return getFaceletHandler(ctx, config, widget, null);
    }

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx,
            TagConfig config, Widget widget, FaceletHandler nextHandler) {
        String widgetTypeName = widget.getType();
        String widgetTypeCategory = widget.getTypeCategory();
        WidgetTypeHandler handler = getWidgetTypeHandler(widgetTypeCategory,
                widgetTypeName);
        if (handler == null) {
            FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
            String message = String.format(
                    "No widget handler found for type '%s' in category '%s'",
                    widgetTypeName, widgetTypeCategory);
            log.error(message);
            ComponentHandler output = helper.getErrorComponentHandler(null,
                    message);
            return output;
        } else {
            FaceletHandler[] subHandlers = null;
            Widget[] subWidgets = widget.getSubWidgets();
            List<FaceletHandler> subHandlersList = new ArrayList<FaceletHandler>();
            if (nextHandler != null) {
                subHandlersList.add(nextHandler);
            }
            if (subWidgets != null) {
                for (Widget subWidget : subWidgets) {
                    subHandlersList.add(getFaceletHandler(ctx, config,
                            subWidget, null));
                }
            }
            if (!subHandlersList.isEmpty()) {
                subHandlers = subHandlersList.toArray(new FaceletHandler[0]);
            }
            FaceletHandler fHandler = handler.getFaceletHandler(ctx, config,
                    widget, subHandlers);
            return fHandler;
        }
    }

    @Override
    public Widget createWidget(FaceletContext ctx, String type, String mode,
            String valueName, Map<String, Serializable> properties,
            Widget[] subWidgets) {
        return createWidget(ctx, type, mode, valueName, null, null, null, null,
                properties, subWidgets);
    }

    @Override
    public Widget createWidget(FaceletContext ctx, String type, String mode,
            String valueName, List<FieldDefinition> fieldDefinitions,
            String label, String helpLabel, Boolean translated,
            Map<String, Serializable> properties, Widget[] subWidgets) {
        return createWidget(
                ctx,
                createWidgetDefinition(ctx, type, null, mode, valueName,
                        fieldDefinitions, null, label, helpLabel, translated,
                        properties, subWidgets), mode, valueName, subWidgets);
    }

    @Override
    public Widget createWidget(FaceletContext ctx, WidgetDefinition wDef,
            String mode, String valueName, Widget[] subWidgets) {

        String wType = wDef.getType();
        String wTypeCat = wDef.getTypeCategory();
        // fill default property values from the widget definition
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        WidgetTypeDefinition def = getLayoutStore().getWidgetTypeDefinition(
                getStoreCategory(wTypeCat), wType);

        boolean required = false;
        WidgetTypeConfiguration conf = def != null ? def.getConfiguration()
                : null;
        if (conf != null) {
            Map<String, Serializable> defaultProps = conf.getDefaultPropertyValues(mode);
            if (defaultProps != null && !defaultProps.isEmpty()) {
                props.putAll(defaultProps);
            }
        }
        Map<String, Serializable> modeProps = wDef.getProperties(mode, mode);
        if (modeProps != null) {
            props.putAll(modeProps);
            Serializable requiredProp = props.get(WidgetDefinition.REQUIRED_PROPERTY_NAME);
            if (requiredProp != null) {
                if (requiredProp instanceof Boolean) {
                    required = ((Boolean) requiredProp).booleanValue();
                } else if (requiredProp instanceof String) {
                    required = getBooleanValue(ctx, (String) requiredProp).booleanValue();
                } else {
                    log.error(String.format(
                            "Invalid property '%s' on widget: '%s'",
                            WidgetDefinition.REQUIRED_PROPERTY_NAME,
                            requiredProp));
                }
            }
        }
        WidgetImpl widget = new WidgetImpl("layout", wDef.getName(), mode,
                wType, valueName, wDef.getFieldDefinitions(),
                wDef.getLabel(mode), wDef.getHelpLabel(mode),
                wDef.isTranslated(), props, required, subWidgets, 0, null,
                LayoutFunctions.computeWidgetDefinitionId(wDef));
        widget.setTypeCategory(wTypeCat);
        return widget;
    }

    protected WidgetDefinition createWidgetDefinition(FaceletContext ctx,
            String type, String category, String mode, String valueName,
            List<FieldDefinition> fieldDefinitions, String widgetName,
            String label, String helpLabel, Boolean translated,
            Map<String, Serializable> properties, Widget[] subWidgets) {
        String wName = widgetName;
        if (StringUtils.isBlank(widgetName)) {
            wName = type;
        }
        WidgetDefinitionImpl wDef = new WidgetDefinitionImpl(wName, type,
                label, helpLabel, Boolean.TRUE.equals(translated), null,
                fieldDefinitions, properties, null);
        return wDef;
    }

    /**
     * @since 5.6
     */
    protected void registerDisabledPropertyRef(
            DisabledPropertyRefDescriptor desc) {
        disabledPropertyRefsReg.addContribution(desc);
        log.info(String.format(
                "Registered disabled property reference descriptor: %s",
                desc.toString()));
    }

    /**
     * @since 5.6
     */
    protected void unregisterDisabledPropertyRef(
            DisabledPropertyRefDescriptor desc) {
        disabledPropertyRefsReg.removeContribution(desc);
        log.info(String.format(
                "Removed disabled property reference descriptor: %s",
                desc.toString()));
    }

    @Override
    public boolean referencePropertyAsExpression(String name,
            Serializable value, String widgetType, String widgetMode,
            String template) {
        return referencePropertyAsExpression(name, value, widgetType, null,
                widgetMode, template);
    }

    @Override
    public boolean referencePropertyAsExpression(String name,
            Serializable value, String widgetType, String widgetTypeCategory,
            String widgetMode, String template) {
        if ((value instanceof String)
                && (ComponentTagUtils.isValueReference((String) value))) {
            return false;
        }
        String cat = widgetTypeCategory;
        if (widgetTypeCategory == null) {
            cat = WebLayoutManager.JSF_CATEGORY;
        }
        for (DisabledPropertyRefDescriptor desc : disabledPropertyRefsReg.getDisabledPropertyRefs()) {
            if (Boolean.TRUE.equals(desc.getEnabled())
                    && desc.matches(name, widgetType, cat, widgetMode, template)) {
                return false;
            }
        }
        return true;
    }

}
