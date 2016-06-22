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
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetType;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
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
import org.nuxeo.ecm.platform.forms.layout.descriptors.LayoutTypeDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetTypeDescriptor;
import org.nuxeo.ecm.platform.forms.layout.facelets.FaceletHandlerHelper;
import org.nuxeo.ecm.platform.forms.layout.facelets.RenderVariables;
import org.nuxeo.ecm.platform.forms.layout.facelets.WidgetTypeHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.dev.DevTagHandler;
import org.nuxeo.ecm.platform.forms.layout.facelets.plugins.TemplateWidgetTypeHandler;
import org.nuxeo.ecm.platform.forms.layout.functions.LayoutFunctions;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;

import com.sun.faces.facelets.el.VariableMapperWrapper;

/**
 * Layout service implementation.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class WebLayoutManagerImpl extends AbstractLayoutManager implements WebLayoutManager {

    public static final ComponentName NAME = new ComponentName(WebLayoutManagerImpl.class.getName());

    private static final Log log = LogFactory.getLog(WebLayoutManagerImpl.class);

    private static final long serialVersionUID = 1L;

    public static final String WIDGET_TYPES_EP_NAME = LayoutStoreImpl.WIDGET_TYPES_EP_NAME;

    /**
     * @since 6.0
     */
    public static final String LAYOUT_TYPES_EP_NAME = LayoutStoreImpl.LAYOUT_TYPES_EP_NAME;

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
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            registerWidgetType(((WidgetTypeDescriptor) contribution).getWidgetTypeDefinition());
        } else if (extensionPoint.equals(LAYOUT_TYPES_EP_NAME)) {
            registerLayoutType(((LayoutTypeDescriptor) contribution).getLayoutTypeDefinition());
        } else if (extensionPoint.equals(LAYOUTS_EP_NAME)) {
            registerLayout(((LayoutDescriptor) contribution).getLayoutDefinition());
        } else if (extensionPoint.equals(WIDGETS_EP_NAME)) {
            registerWidget(((WidgetDescriptor) contribution).getWidgetDefinition());
        } else if (extensionPoint.equals(PROPS_REF_EP_NAME)) {
            registerDisabledPropertyRef(((DisabledPropertyRefDescriptor) contribution));
        } else {
            log.error(String.format("Unknown extension point '%s', can't register !", extensionPoint));
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals(WIDGET_TYPES_EP_NAME)) {
            unregisterWidgetType(((WidgetTypeDescriptor) contribution).getWidgetTypeDefinition());
        } else if (extensionPoint.equals(LAYOUT_TYPES_EP_NAME)) {
            unregisterLayoutType(((LayoutTypeDescriptor) contribution).getLayoutTypeDefinition());
        } else if (extensionPoint.equals(LAYOUTS_EP_NAME)) {
            unregisterLayout(((LayoutDescriptor) contribution).getLayoutDefinition());
        } else if (extensionPoint.equals(WIDGETS_EP_NAME)) {
            unregisterWidget(((WidgetDescriptor) contribution).getWidgetDefinition());
        } else if (extensionPoint.equals(PROPS_REF_EP_NAME)) {
            unregisterDisabledPropertyRef(((DisabledPropertyRefDescriptor) contribution));
        } else {
            log.error(String.format("Unknown extension point '%s', can't unregister !", extensionPoint));
        }
    }

    // specific API (depends on JSF impl)

    @Override
    public String getDefaultStoreCategory() {
        return JSF_CATEGORY;
    }

    @Override
    public WidgetTypeHandler getWidgetTypeHandler(String typeName) throws WidgetException {
        return getWidgetTypeHandler(getDefaultStoreCategory(), typeName);
    }

    @Override
    public WidgetTypeHandler getWidgetTypeHandler(String typeCategory, String typeName) throws WidgetException {
        if (StringUtils.isBlank(typeCategory)) {
            typeCategory = getDefaultStoreCategory();
        }
        WidgetType type = getLayoutStore().getWidgetType(typeCategory, typeName);
        if (type == null) {
            return null;
        }
        WidgetTypeHandler handler;
        Class<?> klass = type.getWidgetTypeClass();
        if (klass == null) {
            // implicit handler is the "template" one
            handler = new TemplateWidgetTypeHandler();
        } else {
            try {
                // Thread context loader is not working in isolated EARs
                handler = (WidgetTypeHandler) klass.newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("Caught error when instanciating widget type handler", e);
                return null;
            }
        }
        // set properties
        handler.setProperties(type.getProperties());
        return handler;
    }

    /**
     * Evaluates an EL expression in given context.
     * <p>
     * If the expression resolves to an EL expression, evaluate it again this is useful when retrieving the expression
     * from a configuration file.
     * <p>
     * If given context is null, do no try to evaluate it and return the expression itself.
     *
     * @param context the facelet context.
     * @param expression the string expression.
     */
    protected static Object evaluateExpression(FaceletContext context, String expression) {
        if (expression == null) {
            return null;
        }
        if (context == null) {
            return expression;
        }
        Object value = ComponentTagUtils.resolveElExpression(context, expression);
        if (value != null && value instanceof String) {
            // evaluate a second time in case it's another EL expression
            value = ComponentTagUtils.resolveElExpression(context, (String) value);
        }
        return value;
    }

    /**
     * Evaluates an expression to a boolean value.
     */
    protected static Boolean getBooleanValue(FaceletContext context, String expression) {
        Object value = evaluateExpression(context, expression);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value == null || value instanceof String) {
            return Boolean.valueOf((String) value);
        } else {
            log.error("Could not get boolean value for '" + value + "' in expression '" + expression + "'");
            return Boolean.FALSE;
        }
    }

    /**
     * Evaluates an expression to a string value.
     */
    protected static String getStringValue(FaceletContext context, String expression) {
        Object value = evaluateExpression(context, expression);
        if (value == null || value instanceof String) {
            return (String) value;
        } else {
            log.error("Could not get string value for '" + value + "' in expression '" + expression + "'");
            return null;
        }
    }

    protected static String getModeFromLayoutMode(FaceletContext context, WidgetDefinition wDef, String layoutMode) {
        String wMode = getStringValue(context, wDef.getMode(layoutMode));
        if (wMode == null) {
            wMode = BuiltinModes.getWidgetModeFromLayoutMode(layoutMode);
        }
        return wMode;
    }

    @Override
    public Widget getWidget(FaceletContext ctx, String widgetName, String widgetCategory, String layoutMode,
            String valueName, String layoutName) {
        WidgetReference widgetRef = new WidgetReferenceImpl(widgetCategory, widgetName);
        WidgetDefinition wDef = lookupWidget(widgetRef);
        return getWidget(ctx, null, null, layoutName, null, wDef, widgetCategory, layoutMode, valueName, 0);
    }

    @Override
    public Widget getWidget(FaceletContext ctx, WidgetDefinition wDef, String layoutMode, String valueName,
            String layoutName) {
        return getWidget(ctx, null, null, layoutName, null, wDef, getDefaultStoreCategory(), layoutMode, valueName, 0);
    }

    @Override
    public Widget getWidget(FaceletContext ctx, LayoutConversionContext lctx, String conversionCat,
            WidgetDefinition widgetDef, String layoutMode, String valueName, String layoutName) {
        return getWidget(ctx, null, null, layoutName, null, widgetDef, getDefaultStoreCategory(), layoutMode,
                valueName, 0);
    }

    /**
     * Computes a widget from a definition for a mode in a given context.
     * <p>
     * If the widget is configured not to be rendered in the given mode, returns null.
     * <p>
     * Sub widgets are also computed recursively.
     */
    @SuppressWarnings("deprecation")
    protected Widget getWidget(FaceletContext context, LayoutConversionContext lctx, String conversionCat,
            String layoutName, LayoutDefinition layoutDef, WidgetDefinition widgetDefinition, String widgetCategory,
            String layoutMode, String valueName, int level) {
        if (widgetDefinition == null) {
            return null;
        }
        WidgetDefinition wDef = widgetDefinition.clone();
        if (lctx != null && !StringUtils.isBlank(conversionCat)) {
            List<WidgetDefinitionConverter> lcs = getLayoutStore().getWidgetConverters(conversionCat);
            for (WidgetDefinitionConverter wc : lcs) {
                wDef = wc.getWidgetDefinition(wDef, lctx);
            }
        }
        VariableMapper orig = null;
        // avoid variable mapper changes if context is null for tests
        if (context != null) {
            // expose widget mode so that it can be used in a mode el
            // expression
            orig = context.getVariableMapper();
            VariableMapper vm = new VariableMapperWrapper(orig);
            context.setVariableMapper(vm);
            ExpressionFactory eFactory = context.getExpressionFactory();
            ValueExpression modeVe = eFactory.createValueExpression(layoutMode, String.class);
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
                Widget subWidget = getWidget(context, lctx, conversionCat, layoutName, layoutDef, swDef,
                        widgetCategory, wMode, valueName, level + 1);
                if (subWidget != null) {
                    subWidgets.add(subWidget);
                }
            }
        }

        WidgetReference[] swRefs = wDef.getSubWidgetReferences();
        if (swRefs != null) {
            for (WidgetReference swRef : swRefs) {
                String cat = swRef.getCategory();
                if (StringUtils.isBlank(cat)) {
                    cat = widgetCategory;
                }
                WidgetDefinition swDef = lookupWidget(layoutDef, new WidgetReferenceImpl(cat, swRef.getName()));
                if (swDef == null) {
                    log.error("Widget '" + swRef.getName() + "' not found in layout " + layoutName);
                } else {
                    Widget subWidget = getWidget(context, lctx, conversionCat, layoutName, layoutDef, swDef, cat,
                            wMode, valueName, level + 1);
                    if (subWidget != null) {
                        subWidgets.add(subWidget);
                    }
                }
            }
        }

        boolean required = getBooleanValue(context, wDef.getRequired(layoutMode, wMode)).booleanValue();

        String wType = wDef.getType();
        String wTypeCat = wDef.getTypeCategory();
        // fill default property and control values from the widget definition
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        Map<String, Serializable> controls = new HashMap<String, Serializable>();
        String actualWTypeCat = getStoreCategory(wTypeCat);
        WidgetTypeDefinition def = getLayoutStore().getWidgetTypeDefinition(actualWTypeCat, wType);

        WidgetTypeConfiguration conf = def != null ? def.getConfiguration() : null;
        if (conf != null) {
            Map<String, Serializable> defaultProps = conf.getDefaultPropertyValues(wMode);
            if (defaultProps != null && !defaultProps.isEmpty()) {
                props.putAll(defaultProps);
            }
            Map<String, Serializable> defaultControls = conf.getDefaultControlValues(wMode);
            if (defaultControls != null && !defaultControls.isEmpty()) {
                controls.putAll(defaultControls);
            }
        }

        props.putAll(wDef.getProperties(layoutMode, wMode));
        controls.putAll(wDef.getControls(layoutMode, wMode));

        WidgetImpl widget = new WidgetImpl(layoutName, wDef.getName(), wMode, wType, valueName,
                wDef.getFieldDefinitions(), wDef.getLabel(layoutMode), wDef.getHelpLabel(layoutMode),
                wDef.isTranslated(), wDef.isHandlingLabels(), props, required, subWidgets.toArray(new Widget[0]),
                level, wDef.getSelectOptions(), LayoutFunctions.computeWidgetDefinitionId(wDef),
                wDef.getRenderingInfos(layoutMode));
        widget.setControls(controls);
        widget.setTypeCategory(actualWTypeCat);
        if (Framework.isDevModeSet()) {
            widget.setDefinition(wDef);
        }
        return widget;
    }

    @Override
    public Layout getLayout(FaceletContext ctx, String layoutName, String mode, String valueName)
            throws LayoutException {
        return getLayout(ctx, layoutName, mode, valueName, null, false);
    }

    @Override
    public Layout getLayout(FaceletContext ctx, String layoutName, String mode, String valueName,
            List<String> selectedRows, boolean selectAllRowsByDefault) {
        return getLayout(ctx, layoutName, null, mode, valueName, selectedRows, selectAllRowsByDefault);
    }

    @Override
    public Layout getLayout(FaceletContext ctx, String layoutName, String layoutCategory, String mode,
            String valueName, List<String> selectedRows, boolean selectAllRowsByDefault) {
        if (StringUtils.isBlank(layoutCategory)) {
            layoutCategory = getDefaultStoreCategory();
        }
        LayoutDefinition layoutDef = getLayoutStore().getLayoutDefinition(layoutCategory, layoutName);
        if (layoutDef == null) {
            if (log.isDebugEnabled()) {
                log.debug("Layout '" + layoutName + "' not found for category '" + layoutCategory + "'");
            }
            return null;
        }
        return getLayout(ctx, layoutDef, mode, valueName, selectedRows, selectAllRowsByDefault);
    }

    @Override
    public Layout getLayout(FaceletContext ctx, LayoutDefinition layoutDef, String mode, String valueName,
            List<String> selectedRows, boolean selectAllRowsByDefault) {
        return getLayout(ctx, null, null, layoutDef, mode, valueName, selectedRows, selectAllRowsByDefault);
    }

    @Override
    public Layout getLayout(FaceletContext ctx, LayoutConversionContext lctx, String conversionCat,
            LayoutDefinition layoutDefinition, String mode, String valueName, List<String> selectedRows,
            boolean selectAllRowsByDefault) {
        if (layoutDefinition == null) {
            log.debug("Layout definition is null");
            return null;
        }
        if (ctx == null) {
            log.warn("Layout creation computed in a null facelet context: expressions "
                    + "found in the layout definition will not be evaluated");
        }
        LayoutDefinition lDef = layoutDefinition.clone();
        if (lctx != null && !StringUtils.isBlank(conversionCat)) {
            List<LayoutDefinitionConverter> lcs = getLayoutStore().getLayoutConverters(conversionCat);
            for (LayoutDefinitionConverter lc : lcs) {
                lDef = lc.getLayoutDefinition(lDef, lctx);
            }
        }
        String layoutName = lDef.getName();
        LayoutRowDefinition[] rowsDef = lDef.getRows();
        List<LayoutRow> rows = new ArrayList<LayoutRow>();
        Set<String> foundRowNames = new HashSet<String>();
        int rowIndex = -1;
        for (LayoutRowDefinition rowDef : rowsDef) {
            rowIndex++;
            String rowName = rowDef.getName();
            if (rowName == null) {
                rowName = rowDef.getDefaultName(rowIndex);
                if (selectedRows != null && log.isDebugEnabled()) {
                    log.debug("Generating default name '" + rowName + "' in layout '" + layoutName
                            + "' for row or column at index " + rowIndex);
                }
            }
            boolean emptyRow = true;
            if (selectedRows != null && !selectedRows.contains(rowName) && !rowDef.isAlwaysSelected()) {
                continue;
            }
            if (selectedRows == null && !selectAllRowsByDefault && !rowDef.isSelectedByDefault()
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
                String cat = widgetRef.getCategory();
                if (StringUtils.isBlank(cat)) {
                    cat = getDefaultStoreCategory();
                }
                WidgetDefinition wDef = lookupWidget(lDef, new WidgetReferenceImpl(cat, widgetName));
                if (wDef == null) {
                    log.error("Widget '" + widgetName + "' not found in layout " + layoutName);
                    widgets.add(null);
                    continue;
                }
                Widget widget = getWidget(ctx, lctx, conversionCat, layoutName, lDef, wDef, cat, mode, valueName, 0);
                if (widget != null) {
                    emptyRow = false;
                }
                widgets.add(widget);
            }
            if (!emptyRow) {
                rows.add(new LayoutRowImpl(rowName, rowDef.isSelectedByDefault(), rowDef.isAlwaysSelected(), widgets,
                        rowDef.getProperties(mode), LayoutFunctions.computeLayoutRowDefinitionId(rowDef)));
            }
            foundRowNames.add(rowName);
        }
        if (selectedRows != null) {
            Collections.sort(rows, new LayoutRowComparator(selectedRows));
            for (String selectedRow : selectedRows) {
                if (!foundRowNames.contains(selectedRow)) {
                    log.warn("Selected row or column named '" + selectedRow + "' " + "was not found in layout '"
                            + layoutName + "'");
                }
            }
        }

        String layoutTypeCategory = lDef.getTypeCategory();
        String actualLayoutTypeCategory = getStoreCategory(layoutTypeCategory);
        LayoutTypeDefinition layoutTypeDef = null;
        String layoutType = lDef.getType();
        if (!StringUtils.isBlank(layoutType)) {
            // retrieve type for templates and props mapping
            layoutTypeDef = getLayoutStore().getLayoutTypeDefinition(actualLayoutTypeCategory, layoutType);
            if (layoutTypeDef == null) {
                log.warn("Layout type '" + layoutType + "' not found for category '" + layoutTypeCategory + "'");
            }
        }

        String template = lDef.getTemplate(mode);
        Map<String, Serializable> props = new HashMap<>();
        if (layoutTypeDef != null) {
            if (StringUtils.isEmpty(template)) {
                template = layoutTypeDef.getTemplate(mode);
            }
            LayoutTypeConfiguration conf = layoutTypeDef.getConfiguration();
            if (conf != null) {
                Map<String, Serializable> typeProps = conf.getDefaultPropertyValues(mode);
                if (typeProps != null) {
                    props.putAll(typeProps);
                }
            }
        }
        Map<String, Serializable> lprops = lDef.getProperties(mode);
        if (lprops != null) {
            props.putAll(lprops);
        }
        LayoutImpl layout = new LayoutImpl(lDef.getName(), mode, template, rows, lDef.getColumns(), props,
                LayoutFunctions.computeLayoutDefinitionId(lDef));
        layout.setValueName(valueName);
        layout.setType(layoutType);
        layout.setTypeCategory(actualLayoutTypeCategory);
        if (Framework.isDevModeSet()) {
            layout.setDefinition(lDef);
            // resolve template in "dev" mode, avoiding default lookup on "any"
            // mode
            Map<String, String> templates = lDef.getTemplates();
            String devTemplate = templates != null ? templates.get(BuiltinModes.DEV) : null;
            if (layoutTypeDef != null && StringUtils.isEmpty(devTemplate)) {
                Map<String, String> typeTemplates = layoutTypeDef.getTemplates();
                devTemplate = typeTemplates != null ? typeTemplates.get(BuiltinModes.DEV) : null;
            }
            layout.setDevTemplate(devTemplate);
        }
        return layout;
    }

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx, TagConfig config, Widget widget) {
        return getFaceletHandler(ctx, config, widget, null);
    }

    @Override
    public FaceletHandler getFaceletHandler(FaceletContext ctx, TagConfig config, Widget widget,
            FaceletHandler nextHandler) {
        String widgetTypeName = widget.getType();
        String widgetTypeCategory = widget.getTypeCategory();
        WidgetTypeHandler handler = getWidgetTypeHandler(widgetTypeCategory, widgetTypeName);
        if (handler == null) {
            FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
            String message = "No widget handler found for type '" + widgetTypeName + "' in category '"
                    + widgetTypeCategory + "'";
            log.error(message);
            ComponentHandler output = helper.getErrorComponentHandler(null, message);
            return output;
        } else {
            FaceletHandler[] subHandlers = null;
            List<FaceletHandler> subHandlersList = new ArrayList<FaceletHandler>();
            if (nextHandler != null) {
                subHandlersList.add(nextHandler);
            }
            if (!subHandlersList.isEmpty()) {
                subHandlers = subHandlersList.toArray(new FaceletHandler[0]);
            }
            FaceletHandler widgetHandler = handler.getFaceletHandler(ctx, config, widget, subHandlers);

            if (FaceletHandlerHelper.isDevModeEnabled(ctx)) {
                // decorate handler with dev handler
                FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
                FaceletHandler devHandler = handler.getDevFaceletHandler(ctx, config, widget);
                if (devHandler == null) {
                    return widgetHandler;
                }
                // expose the widget variable to sub dev handler
                String widgetTagConfigId = widget.getTagConfigId();
                Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
                ExpressionFactory eFactory = ctx.getExpressionFactory();
                ValueExpression widgetVe = eFactory.createValueExpression(widget, Widget.class);
                variables.put(RenderVariables.widgetVariables.widget.name(), widgetVe);
                List<String> blockedPatterns = new ArrayList<String>();
                blockedPatterns.add(RenderVariables.widgetVariables.widget.name() + "*");
                FaceletHandler devAliasHandler = helper.getAliasTagHandler(widgetTagConfigId, variables,
                        blockedPatterns, devHandler);
                String refId = widget.getName();
                FaceletHandler widgetDevHandler = new DevTagHandler(config, refId, widgetHandler, devAliasHandler);
                return widgetDevHandler;
            }
            return widgetHandler;
        }
    }

    @Override
    public Widget createWidget(FaceletContext ctx, String type, String mode, String valueName,
            Map<String, Serializable> properties, Widget[] subWidgets) {
        return createWidget(ctx, type, mode, valueName, null, null, null, null, properties, subWidgets);
    }

    @Override
    public Widget createWidget(FaceletContext ctx, String type, String mode, String valueName,
            List<FieldDefinition> fieldDefinitions, String label, String helpLabel, Boolean translated,
            Map<String, Serializable> properties, Widget[] subWidgets) {
        return createWidget(
                ctx,
                createWidgetDefinition(ctx, type, null, mode, valueName, fieldDefinitions, null, label, helpLabel,
                        translated, properties, subWidgets), mode, valueName, subWidgets);
    }

    @Override
    public Widget createWidget(FaceletContext ctx, WidgetDefinition wDef, String mode, String valueName,
            Widget[] subWidgets) {

        String wType = wDef.getType();
        String wTypeCat = wDef.getTypeCategory();
        // fill default property and control values from the widget definition
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        Map<String, Serializable> controls = new HashMap<String, Serializable>();
        String actualWTypeCat = getStoreCategory(wTypeCat);
        WidgetTypeDefinition def = getLayoutStore().getWidgetTypeDefinition(actualWTypeCat, wType);

        boolean required = false;
        WidgetTypeConfiguration conf = def != null ? def.getConfiguration() : null;
        if (conf != null) {
            Map<String, Serializable> defaultProps = conf.getDefaultPropertyValues(mode);
            if (defaultProps != null && !defaultProps.isEmpty()) {
                props.putAll(defaultProps);
            }
            Map<String, Serializable> defaultControls = conf.getDefaultControlValues(mode);
            if (defaultControls != null && !defaultControls.isEmpty()) {
                controls.putAll(defaultControls);
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
                    log.error("Invalid property 'required' on widget: '" + requiredProp + "'.");
                }
            }
        }
        Map<String, Serializable> modeControls = wDef.getControls(mode, mode);
        if (modeControls != null) {
            controls.putAll(modeControls);
        }
        WidgetImpl widget = new WidgetImpl("layout", wDef.getName(), mode, wType, valueName,
                wDef.getFieldDefinitions(), wDef.getLabel(mode), wDef.getHelpLabel(mode), wDef.isTranslated(), props,
                required, subWidgets, 0, null, LayoutFunctions.computeWidgetDefinitionId(wDef));
        widget.setControls(controls);
        widget.setTypeCategory(actualWTypeCat);
        widget.setDynamic(wDef.isDynamic());
        widget.setGlobal(wDef.isGlobal());
        if (Framework.isDevModeSet()) {
            widget.setDefinition(wDef);
        }
        return widget;
    }

    protected WidgetDefinition createWidgetDefinition(FaceletContext ctx, String type, String category, String mode,
            String valueName, List<FieldDefinition> fieldDefinitions, String widgetName, String label,
            String helpLabel, Boolean translated, Map<String, Serializable> properties, Widget[] subWidgets) {
        String wName = widgetName;
        if (StringUtils.isBlank(widgetName)) {
            wName = type;
        }
        WidgetDefinitionImpl wDef = new WidgetDefinitionImpl(wName, type, label, helpLabel,
                Boolean.TRUE.equals(translated), null, fieldDefinitions, properties, null);
        wDef.setDynamic(true);
        return wDef;
    }

    /**
     * @since 5.6
     */
    protected void registerDisabledPropertyRef(DisabledPropertyRefDescriptor desc) {
        disabledPropertyRefsReg.addContribution(desc);
        log.info(String.format("Registered disabled property reference descriptor: %s", desc.toString()));
    }

    /**
     * @since 5.6
     */
    protected void unregisterDisabledPropertyRef(DisabledPropertyRefDescriptor desc) {
        disabledPropertyRefsReg.removeContribution(desc);
        log.info(String.format("Removed disabled property reference descriptor: %s", desc.toString()));
    }

    @Override
    public boolean referencePropertyAsExpression(String name, Serializable value, String widgetType, String widgetMode,
            String template) {
        return referencePropertyAsExpression(name, value, widgetType, null, widgetMode, template);
    }

    @Override
    public boolean referencePropertyAsExpression(String name, Serializable value, String widgetType,
            String widgetTypeCategory, String widgetMode, String template) {
        if ((value instanceof String) && (ComponentTagUtils.isValueReference((String) value))) {
            return false;
        }
        String cat = widgetTypeCategory;
        if (widgetTypeCategory == null) {
            cat = WebLayoutManager.JSF_CATEGORY;
        }
        for (DisabledPropertyRefDescriptor desc : disabledPropertyRefsReg.getDisabledPropertyRefs()) {
            if (Boolean.TRUE.equals(desc.getEnabled()) && desc.matches(name, widgetType, cat, widgetMode, template)) {
                return false;
            }
        }
        return true;
    }

}
