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
 * $Id: WidgetTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.MetaTagHandler;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.ValueExpressionLiteral;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.binding.BlockingVariableMapper;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.facelets.el.VariableMapperWrapper;

/**
 * Widget tag handler.
 * <p>
 * Applies {@link WidgetTypeHandler} found for given widget, in given mode and for given value.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class WidgetTagHandler extends MetaTagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WidgetTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute widget;

    /**
     * @since 5.6
     */
    protected final TagAttribute name;

    /**
     * @since 5.6
     */
    protected final TagAttribute category;

    /**
     * @since 5.6
     */
    protected final TagAttribute definition;

    /**
     * @since 5.6
     */
    protected final TagAttribute mode;

    /**
     * @since 5.6
     */
    protected final TagAttribute layoutName;

    /**
     * @since 5.7
     */
    protected final TagAttribute resolveOnly;

    protected final TagAttribute value;

    protected final TagAttribute[] vars;

    protected final String[] reservedVarsArray = { "id", "widget", "name", "category", "definition", "mode",
            "layoutName", "value", "resolveOnly" };

    public WidgetTagHandler(TagConfig config) {
        super(config);
        this.config = config;

        widget = getAttribute("widget");
        name = getAttribute("name");
        definition = getAttribute("definition");
        category = getAttribute("category");
        mode = getAttribute("mode");
        layoutName = getAttribute("layoutName");
        resolveOnly = getAttribute("resolveOnly");

        value = getAttribute("value");
        vars = tag.getAttributes().getAll();

        // additional checks
        if (name == null && widget == null && definition == null) {
            throw new TagException(this.tag,
                    "At least one of attributes 'name', 'widget' " + "or 'definition' is required");
        }
        if (widget == null && (name != null || definition != null)) {
            if (mode == null) {
                throw new TagException(this.tag, "Attribute 'mode' is required when using attribute"
                        + " 'name' or 'definition' so that the " + "widget instance " + "can be resolved");
            }
        }
    }

    /**
     * Renders given widget resolving its {@link FaceletHandler} from {@link WebLayoutManager} configuration.
     * <p>
     * Variables exposed: {@link RenderVariables.globalVariables#value}, same variable suffixed with "_n" where n is the
     * widget level, and {@link RenderVariables.globalVariables#document}.
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException {
        // compute value name to set on widget instance in case it's changed
        // from first computation
        String valueName = null;
        if (value != null) {
            valueName = value.getValue();
        }
        if (ComponentTagUtils.isStrictValueReference(valueName)) {
            valueName = ComponentTagUtils.getBareValueName(valueName);
        }

        // build handler
        boolean widgetInstanceBuilt = false;
        Widget widgetInstance = null;
        if (widget != null) {
            widgetInstance = (Widget) widget.getObject(ctx, Widget.class);
            if (widgetInstance != null && valueName != null) {
                widgetInstance.setValueName(valueName);
            }
        } else {
            // resolve widget according to name and mode (and optional
            // category)
            WebLayoutManager layoutService = Framework.getService(WebLayoutManager.class);

            String modeValue = mode.getValue(ctx);
            String layoutNameValue = null;
            if (layoutName != null) {
                layoutNameValue = layoutName.getValue(ctx);
            }

            if (name != null) {
                String nameValue = name.getValue(ctx);
                String catValue = null;
                if (category != null) {
                    catValue = category.getValue(ctx);
                }
                widgetInstance = layoutService.getWidget(ctx, nameValue, catValue, modeValue, valueName,
                        layoutNameValue);
                widgetInstanceBuilt = true;
            } else if (definition != null) {
                WidgetDefinition widgetDef = (WidgetDefinition) definition.getObject(ctx, WidgetDefinition.class);
                if (widgetDef != null) {
                    widgetInstance = layoutService.getWidget(ctx, widgetDef, modeValue, valueName, layoutNameValue);
                    widgetInstanceBuilt = true;
                }
            }

        }
        if (widgetInstance != null) {
            // add additional properties put on tag
            String widgetPropertyMarker = RenderVariables.widgetVariables.widgetProperty.name() + "_";
            List<String> reservedVars = Arrays.asList(reservedVarsArray);
            for (TagAttribute var : vars) {
                String localName = var.getLocalName();
                if (!reservedVars.contains(localName)) {
                    if (localName != null && localName.startsWith(widgetPropertyMarker)) {
                        localName = localName.substring(widgetPropertyMarker.length());
                    }
                    widgetInstance.setProperty(localName, var.getValue());
                }
            }

            VariableMapper orig = ctx.getVariableMapper();
            try {
                if (FaceletHandlerHelper.isAliasOptimEnabled()) {
                    applyOptimized(ctx, orig, widgetInstance, widgetInstanceBuilt);
                } else {
                    applyCompat(ctx, orig, widgetInstance, widgetInstanceBuilt);
                }

                boolean resolveOnlyBool = false;
                if (resolveOnly != null) {
                    resolveOnlyBool = resolveOnly.getBoolean(ctx);
                }

                if (resolveOnlyBool) {
                    nextHandler.apply(ctx, parent);
                } else {
                    applyWidgetHandler(ctx, parent, config, widgetInstance, value, true, nextHandler);
                }
            } finally {
                ctx.setVariableMapper(orig);
            }
        }
    }

    public static void generateWidgetIdsRecursive(FaceletHandlerHelper helper, Widget widget) {
        generateWidgetId(helper, widget, true);
    }

    /**
     * @since 7.2
     */
    public static void generateWidgetId(FaceletHandlerHelper helper, Widget widget, boolean recursive) {
        if (widget == null) {
            return;
        }
        widget.setId(helper.generateWidgetId(widget.getName()));
        if (recursive) {
            Widget[] subWidgets = widget.getSubWidgets();
            if (subWidgets != null) {
                for (Widget subWidget : subWidgets) {
                    generateWidgetIdsRecursive(helper, subWidget);
                }
            }
        }
    }

    protected void applyOptimized(FaceletContext ctx, VariableMapper orig, Widget widgetInstance,
            boolean widgetInstanceBuilt) {
        BlockingVariableMapper vm = new BlockingVariableMapper(orig);
        ctx.setVariableMapper(vm);

        if (widgetInstanceBuilt) {
            // expose widget variable to the context as layout row has not done it already, and set unique id on
            // widget before exposing it to the context
            FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
            WidgetTagHandler.generateWidgetId(helper, widgetInstance, false);
            exposeWidgetVariables(ctx, vm, widgetInstance, null, false);
        }

    }

    protected void applyCompat(FaceletContext ctx, VariableMapper orig, Widget widgetInstance,
            boolean widgetInstanceBuilt) {
        // expose widget variable to the context as layout row has not done it already, and set unique id on
        // widget before exposing it to the context
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        WidgetTagHandler.generateWidgetId(helper, widgetInstance, false);

        VariableMapper vm = new VariableMapperWrapper(orig);
        ctx.setVariableMapper(vm);
        ExpressionFactory eFactory = ctx.getExpressionFactory();
        ValueExpression widgetVe = eFactory.createValueExpression(widgetInstance, Widget.class);
        vm.setVariable(RenderVariables.widgetVariables.widget.name(), widgetVe);
        // expose widget controls too
        for (Map.Entry<String, Serializable> ctrl : widgetInstance.getControls().entrySet()) {
            String key = ctrl.getKey();
            String name = RenderVariables.widgetVariables.widgetControl.name() + "_" + key;
            String value = "#{" + RenderVariables.widgetVariables.widget.name() + ".controls." + key + "}";
            vm.setVariable(name, eFactory.createValueExpression(ctx, value, Object.class));
        }
    }

    public static void applyWidgetHandler(FaceletContext ctx, UIComponent parent, TagConfig config, Widget widget,
            TagAttribute value, boolean fillVariables, FaceletHandler nextHandler) throws IOException {
        if (widget == null) {
            return;
        }
        WebLayoutManager layoutService = Framework.getService(WebLayoutManager.class);

        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        FaceletHandler handler = layoutService.getFaceletHandler(ctx, config, widget, nextHandler);
        if (handler == null) {
            return;
        }

        if (FaceletHandlerHelper.isAliasOptimEnabled()) {
            if (fillVariables) {
                // expose widget variables
                VariableMapper cvm = ctx.getVariableMapper();
                if (!(cvm instanceof BlockingVariableMapper)) {
                    throw new IllegalArgumentException(
                            "Current context variable mapper should be an instance of MetaVariableMapper");
                }
                BlockingVariableMapper vm = (BlockingVariableMapper) cvm;
                ValueExpression valueExpr;
                if (value == null) {
                    valueExpr = new ValueExpressionLiteral(null, Object.class);
                } else {
                    valueExpr = value.getValueExpression(ctx, Object.class);
                }

                vm.setVariable(RenderVariables.globalVariables.value.name(), valueExpr);
                vm.setVariable(RenderVariables.globalVariables.value.name() + "_" + widget.getLevel(), valueExpr);
            }
            handler.apply(ctx, parent);
        } else {
            if (fillVariables) {
                // expose widget variables
                Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();

                ValueExpression valueExpr;
                if (value == null) {
                    valueExpr = new ValueExpressionLiteral(null, Object.class);
                } else {
                    valueExpr = value.getValueExpression(ctx, Object.class);
                }

                variables.put(RenderVariables.globalVariables.value.name(), valueExpr);
                variables.put(RenderVariables.globalVariables.value.name() + "_" + widget.getLevel(), valueExpr);

                FaceletHandler handlerWithVars = helper.getAliasTagHandler(widget.getTagConfigId(), variables, null,
                        handler);
                // apply
                handlerWithVars.apply(ctx, parent);

            } else {
                // just apply
                handler.apply(ctx, parent);
            }
        }
    }

    public static void exposeWidgetVariables(FaceletContext ctx, BlockingVariableMapper vm, Widget widget,
            Integer widgetIndex, boolean exposeLevel) {
        ExpressionFactory eFactory = ctx.getExpressionFactory();
        ValueExpression widgetVe = eFactory.createValueExpression(widget, Widget.class);
        vm.setVariable(RenderVariables.widgetVariables.widget.name(), widgetVe);
        vm.addBlockedPattern(RenderVariables.widgetVariables.widget.name());

        ValueExpression widgetIndexVe = null;
        if (widgetIndex != null) {
            widgetIndexVe = eFactory.createValueExpression(widgetIndex, Integer.class);
            vm.setVariable(RenderVariables.widgetVariables.widgetIndex.name(), widgetIndexVe);
        }

        if (exposeLevel && !FaceletHandlerHelper.isAliasOptimEnabled()) {
            Integer level = null;
            if (widget != null) {
                level = widget.getLevel();
            }
            vm.setVariable(RenderVariables.widgetVariables.widget.name() + "_" + level, widgetVe);
            if (widgetIndexVe != null) {
                vm.setVariable(RenderVariables.widgetVariables.widgetIndex.name() + "_" + level, widgetIndexVe);
            }
            vm.addBlockedPattern(RenderVariables.widgetVariables.widget.name() + "_*");
            vm.addBlockedPattern(RenderVariables.widgetVariables.widgetIndex.name() + "*");
        }

        // expose widget controls too
        if (widget != null) {
            for (Map.Entry<String, Serializable> ctrl : widget.getControls().entrySet()) {
                String key = ctrl.getKey();
                String name = RenderVariables.widgetVariables.widgetControl.name() + "_" + key;
                ValueExpression ve = eFactory.createValueExpression(ctrl.getValue(), Object.class);
                vm.setVariable(name, ve);
            }
        }
        vm.addBlockedPattern(RenderVariables.widgetVariables.widgetControl.name() + "_*");
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        return null;
    }

}
