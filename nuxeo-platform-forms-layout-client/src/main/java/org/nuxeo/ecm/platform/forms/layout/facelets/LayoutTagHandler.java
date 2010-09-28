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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagException;
import com.sun.facelets.tag.TagHandler;
import com.sun.facelets.tag.jsf.ComponentHandler;

/**
 * Layout tag handler.
 * <p>
 * Computes a layout in given facelet context, for given mode and value
 * attributes.
 * <p>
 * If a template is found for this widget, include the corresponding facelet
 * and use facelet template features to iterate over rows and widgets.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutTagHandler extends TagHandler {

    private static final Log log = LogFactory.getLog(LayoutTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute name;

    protected final TagAttribute mode;

    protected final TagAttribute value;

    protected final TagAttribute template;

    protected final TagAttribute selectedRows;

    protected final TagAttribute selectedColumns;

    protected final TagAttribute selectDefaultRows;

    protected final TagAttribute selectDefaultColumns;

    protected final TagAttribute[] vars;

    protected final String[] reservedVarsArray = { "id", "name", "mode",
            "value", "template", "selectedRows", "selectedColumns",
            "selectDefaultRows", "selectDefaultColumns" };

    public LayoutTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        name = getRequiredAttribute("name");
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
        selectDefaultRows = getAttribute("selectDefaultRows");
        selectDefaultColumns = getAttribute("selectDefaultColumns");
        if (selectDefaultRows != null && selectDefaultColumns != null) {
            throw new TagException(this.tag, "Attributes 'selectDefaultRows' "
                    + "and 'selectDefaultColumns' are aliases: only one of "
                    + "them should be filled");
        }
        vars = tag.getAttributes().getAll();
    }

    /**
     * If resolved layout has a template, apply it, else apply widget type
     * handlers for widgets, ignoring rows.
     */
    // TODO: add javadoc about variables exposed
    @SuppressWarnings("unchecked")
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        WebLayoutManager layoutService;
        try {
            layoutService = Framework.getService(WebLayoutManager.class);
        } catch (Exception e) {
            throw new FacesException(e);
        }

        String layoutName = name.getValue(ctx);
        String modeValue = mode.getValue(ctx);
        String valueName = value.getValue();
        if (ComponentTagUtils.isValueReference(valueName)) {
            valueName = valueName.substring(2, valueName.length() - 1);
        }

        // expose some layout variables before layout creation so that they can
        // be used in mode expressions
        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = new VariableMapperWrapper(orig);
        ctx.setVariableMapper(vm);
        ValueExpression valueExpr = value.getValueExpression(ctx, Object.class);
        vm.setVariable(RenderVariables.globalVariables.value.name(), valueExpr);
        vm.setVariable(RenderVariables.globalVariables.document.name(),
                valueExpr);
        vm.setVariable(RenderVariables.globalVariables.layoutValue.name(),
                valueExpr);
        ExpressionFactory eFactory = ctx.getExpressionFactory();
        ValueExpression modeVe = eFactory.createValueExpression(modeValue,
                String.class);
        vm.setVariable(RenderVariables.globalVariables.layoutMode.name(),
                modeVe);
        // mode as alias to layoutMode
        vm.setVariable(RenderVariables.globalVariables.mode.name(), modeVe);

        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);

        List<String> selectedRowsValue = null;
        if (selectedRows != null || selectedColumns != null) {
            if (selectedRows != null) {
                selectedRowsValue = (List<String>) selectedRows.getObject(ctx,
                        List.class);
            } else if (selectedColumns != null) {
                selectedRowsValue = (List<String>) selectedColumns.getObject(
                        ctx, List.class);
            }
        }
        boolean selectDefaultRowsValue = false;
        if (selectDefaultRows != null || selectDefaultColumns != null) {
            if (selectDefaultRows != null) {
                selectDefaultRowsValue = selectDefaultRows.getBoolean(ctx);
            } else if (selectDefaultColumns != null) {
                selectDefaultRowsValue = selectDefaultColumns.getBoolean(ctx);
            }
        }
        Layout layout = layoutService.getLayout(ctx, layoutName, modeValue,
                valueName, selectedRowsValue, selectDefaultRowsValue);
        if (layout == null) {
            String errMsg = String.format("Layout '%s' not found", layoutName);
            log.error(errMsg);
            // display an error message on interface
            FaceletHandler leaf = new LeafFaceletHandler();
            TagAttribute valueAttr = helper.createAttribute("value",
                    "<span style=\"color:red;font-weight:bold;\">ERROR: "
                            + errMsg + "</span><br />");
            TagAttribute escapeAttr = helper.createAttribute("escape", "false");
            ComponentHandler output = helper.getHtmlComponentHandler(
                    FaceletHandlerHelper.getTagAttributes(valueAttr, escapeAttr),
                    leaf, HtmlOutputText.COMPONENT_TYPE, null);
            output.apply(ctx, parent);
            return;
        }

        // set unique id on layout
        layout.setId(helper.generateLayoutId(layout.getName()));
        // add additional properties put on tag
        List<String> reservedVars = Arrays.asList(reservedVarsArray);
        for (TagAttribute var : vars) {
            String localName = var.getLocalName();
            if (!reservedVars.contains(localName)) {
                layout.setProperty(localName, var.getValue());
            }
        }

        // expose layout value
        ValueExpression layoutVe = eFactory.createValueExpression(layout,
                Layout.class);
        vm.setVariable(RenderVariables.layoutVariables.layout.name(), layoutVe);
        // expose layout properties
        for (Map.Entry<String, Serializable> prop : layout.getProperties().entrySet()) {
            vm.setVariable(String.format("%s_%s",
                    RenderVariables.layoutVariables.layoutProperty.name(),
                    prop.getKey()), eFactory.createValueExpression(
                    prop.getValue(), Serializable.class));
        }

        String templateValue = null;
        if (template != null) {
            templateValue = template.getValue(ctx);
        }
        if (templateValue == null || "".equals(templateValue)) {
            templateValue = layout.getTemplate();
        }
        try {
            if (templateValue != null && !"".equals(templateValue)) {
                ctx.includeFacelet(parent, templateValue);
            } else {
                log.error("Missing template property for layout " + layoutName
                        + " => applying basic template");
                for (LayoutRow row : layout.getRows()) {
                    for (Widget widget : row.getWidgets()) {
                        WidgetTagHandler.applyWidgetHandler(ctx, parent,
                                config, widget, value, false);
                    }
                }
            }
        } finally {
            ctx.setVariableMapper(orig);
        }

    }
}
