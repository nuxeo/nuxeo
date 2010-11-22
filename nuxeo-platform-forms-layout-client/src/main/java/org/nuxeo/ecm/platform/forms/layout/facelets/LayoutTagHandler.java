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
 * $Id: LayoutTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.exceptions.WidgetException;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.CompositeFaceletHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagException;
import com.sun.facelets.tag.TagHandler;
import com.sun.facelets.tag.jsf.ComponentHandler;
import com.sun.facelets.tag.ui.IncludeHandler;
import com.sun.facelets.tag.ui.ParamHandler;

/**
 * Layout tag handler.
 * <p>
 * Computes a layout in given facelet context, for given mode and value
 * attributes. The layout can either be computed from a layout definition, or
 * by a layout name, where the ayout service will lookup the corresponding
 * definition.
 * <p>
 * If a template is found for this layout, include the corresponding facelet
 * and use facelet template features to iterate over rows and widgets.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutTagHandler extends TagHandler {

    private static final Log log = LogFactory.getLog(LayoutTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute name;

    protected final TagAttribute definition;

    protected final TagAttribute mode;

    protected final TagAttribute value;

    protected final TagAttribute template;

    protected final TagAttribute selectedRows;

    protected final TagAttribute selectedColumns;

    protected final TagAttribute selectAllByDefault;

    protected final TagAttribute[] vars;

    protected final String[] reservedVarsArray = { "id", "name", "definition",
            "mode", "value", "template", "selectedRows", "selectedColumns",
            "selectAllByDefault" };

    public LayoutTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        name = getAttribute("name");
        definition = getAttribute("definition");
        if (name == null && definition == null) {
            throw new TagException(this.tag,
                    "At least one of attributes 'name', 'definition' or 'layout'"
                            + " is required");
        }
        mode = getRequiredAttribute("mode");
        value = getRequiredAttribute("value");
        template = getAttribute("template");
        selectedRows = getAttribute("selectedRows");
        selectedColumns = getAttribute("selectedColumns");
        if (selectedRows != null && selectedColumns != null) {
            throw new TagException(this.tag, "Attributes 'selectedRows' "
                    + "and 'selectedColumns' are aliases: only one of "
                    + "them should be filled");
        }
        selectAllByDefault = getAttribute("selectAllByDefault");
        vars = tag.getAttributes().getAll();
    }

    /**
     * If resolved layout has a template, apply it, else apply widget type
     * handlers for widgets, ignoring rows.
     */
    // TODO: add javadoc about variables exposed
    @SuppressWarnings("unchecked")
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, ELException {
        WebLayoutManager layoutService;
        try {
            layoutService = Framework.getService(WebLayoutManager.class);
        } catch (Exception e) {
            throw new FacesException(e);
        }
        if (layoutService == null) {
            throw new FacesException("Layout service not found");
        }

        String modeValue = mode.getValue(ctx);
        String valueName = value.getValue();
        if (ComponentTagUtils.isValueReference(valueName)) {
            valueName = valueName.substring(2, valueName.length() - 1);
        }

        // expose some layout variables before layout creation so that they
        // can be used in mode expressions
        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = new VariableMapperWrapper(orig);
        ctx.setVariableMapper(vm);
        Map<String, ValueExpression> variables = getVariablesForLayoutBuild(
                ctx, modeValue);

        String layoutName = null;
        Layout layoutInstance = null;
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        try {
            for (Map.Entry<String, ValueExpression> var : variables.entrySet()) {
                vm.setVariable(var.getKey(), var.getValue());
            }
            List<String> selectedRowsValue = null;
            if (selectedRows != null || selectedColumns != null) {
                if (selectedRows != null) {
                    selectedRowsValue = (List<String>) selectedRows.getObject(
                            ctx, List.class);
                } else if (selectedColumns != null) {
                    selectedRowsValue = (List<String>) selectedColumns.getObject(
                            ctx, List.class);
                }
            }
            boolean selectAllByDefaultValue = false;
            if (selectAllByDefault != null) {
                selectAllByDefaultValue = selectAllByDefault.getBoolean(ctx);
            }

            if (name != null) {
                layoutName = name.getValue(ctx);
                layoutInstance = layoutService.getLayout(ctx, layoutName,
                        modeValue, valueName, selectedRowsValue,
                        selectAllByDefaultValue);
            } else if (definition != null) {
                LayoutDefinition layoutDef = (LayoutDefinition) definition.getObject(
                        ctx, LayoutDefinition.class);
                if (layoutDef == null) {
                    String errMsg = "Layout definition resolved to null";
                    log.error(errMsg);
                    ComponentHandler output = helper.getErrorComponentHandler(errMsg);
                    output.apply(ctx, parent);
                    return;
                }
                layoutName = layoutDef.getName();
                layoutInstance = layoutService.getLayout(ctx, layoutDef,
                        modeValue, valueName, selectedRowsValue,
                        selectAllByDefaultValue);
            }

        } finally {
            // layout resolved => cleanup variable mapper
            ctx.setVariableMapper(orig);
        }

        if (layoutInstance == null) {
            String errMsg = String.format("Layout '%s' not found", layoutName);
            log.error(errMsg);
            ComponentHandler output = helper.getErrorComponentHandler(errMsg);
            output.apply(ctx, parent);
            return;
        }

        // set unique id on layout
        layoutInstance.setId(helper.generateLayoutId(layoutInstance.getName()));

        // add additional properties put on tag
        List<String> reservedVars = Arrays.asList(reservedVarsArray);
        for (TagAttribute var : vars) {
            String localName = var.getLocalName();
            if (!reservedVars.contains(localName)) {
                layoutInstance.setProperty(localName, var.getValue());
            }
        }

        String templateValue = null;
        if (template != null) {
            templateValue = template.getValue(ctx);
        }
        if (templateValue == null || "".equals(templateValue)) {
            templateValue = layoutInstance.getTemplate();
        }
        if (templateValue != null && !"".equals(templateValue)) {

            TagConfig config = TagConfigFactory.createTagConfig(
                    this.config,
                    FaceletHandlerHelper.getTagAttributes(helper.createAttribute(
                            "src", templateValue)), getNextHandler(ctx,
                            this.config, helper, layoutInstance));
            FaceletHandler includeHandler = new IncludeHandler(config);

            // expose layout variables
            variables.putAll(getVariablesForLayout(ctx, layoutInstance));
            FaceletHandler handler = helper.getAliasTagHandler(variables,
                    includeHandler);

            // apply
            handler.apply(ctx, parent);

        } else {
            log.error("Missing template property for layout " + layoutName);
        }

    }

    protected Map<String, ValueExpression> getVariablesForLayoutBuild(
            FaceletContext ctx, String modeValue) {
        Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
        ValueExpression valueExpr = value.getValueExpression(ctx, Object.class);
        variables.put(RenderVariables.globalVariables.value.name(), valueExpr);
        variables.put(RenderVariables.globalVariables.document.name(),
                valueExpr);
        variables.put(RenderVariables.globalVariables.layoutValue.name(),
                valueExpr);
        ExpressionFactory eFactory = ctx.getExpressionFactory();
        ValueExpression modeVe = eFactory.createValueExpression(modeValue,
                String.class);
        variables.put(RenderVariables.globalVariables.layoutMode.name(), modeVe);
        // mode as alias to layoutMode
        variables.put(RenderVariables.globalVariables.mode.name(), modeVe);
        return variables;
    }

    protected Map<String, ValueExpression> getVariablesForLayout(
            FaceletContext ctx, Layout layoutInstance) {
        Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
        ExpressionFactory eFactory = ctx.getExpressionFactory();
        // expose layout value
        ValueExpression layoutVe = eFactory.createValueExpression(
                layoutInstance, Layout.class);
        variables.put(RenderVariables.layoutVariables.layout.name(), layoutVe);
        return variables;
    }

    protected FaceletHandler getNextHandler(FaceletContext ctx,
            TagConfig tagConfig, FaceletHandlerHelper helper,
            Layout layoutInstance) throws WidgetException {
        FaceletHandler leaf = new LeafFaceletHandler();
        List<ParamHandler> paramHandlers = new ArrayList<ParamHandler>();

        {
            TagAttribute name = helper.createAttribute("name",
                    RenderVariables.layoutVariables.layout.name());
            TagAttribute value = helper.createAttribute("value", "#{layout}");
            TagConfig config = TagConfigFactory.createTagConfig(tagConfig,
                    FaceletHandlerHelper.getTagAttributes(name, value), leaf);
            paramHandlers.add(new ParamHandler(config));
        }

        // expose layout properties too
        for (Map.Entry<String, Serializable> prop : layoutInstance.getProperties().entrySet()) {
            String key = prop.getKey();
            TagAttribute name = helper.createAttribute("name", String.format(
                    "%s_%s",
                    RenderVariables.layoutVariables.layoutProperty.name(), key));
            TagAttribute value;
            Serializable valueInstance = prop.getValue();
            if (!helper.shouldCreateReferenceAttribute(key, valueInstance)) {
                // FIXME: this will not be updated correctly using ajax
                value = helper.createAttribute("value", (String) valueInstance);
            } else {
                // create a reference so that it's a real expression and it's
                // not kept (cached) in a component value on ajax refresh
                value = helper.createAttribute("value", String.format(
                        "#{%s.properties.%s}",
                        RenderVariables.layoutVariables.layout.name(), key));
            }
            TagConfig config = TagConfigFactory.createTagConfig(tagConfig,
                    FaceletHandlerHelper.getTagAttributes(name, value), leaf);
            paramHandlers.add(new ParamHandler(config));
        }

        List<FaceletHandler> children = new ArrayList<FaceletHandler>();
        children.addAll(paramHandlers);
        children.add(nextHandler);
        return new CompositeFaceletHandler(
                children.toArray(new FaceletHandler[] {}));
    }

}
