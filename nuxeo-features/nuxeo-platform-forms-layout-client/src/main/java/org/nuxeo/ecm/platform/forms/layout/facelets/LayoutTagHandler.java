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

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

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
import com.sun.facelets.TemplateClient;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * Layout tag handler.
 * <p>
 * Computes a layout in given facelet context, for given mode and value
 * attributes.
 * <p>
 * If a template is found for this widget, include the corresponding facelet and
 * use facelet template features to iterate over rows and widgets.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutTagHandler extends TagHandler implements TemplateClient {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(LayoutTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute name;

    protected final TagAttribute mode;

    protected final TagAttribute value;

    // composition attributes

    protected Layout layout = null;

    int rowCounter = 0;

    int widgetCounter = 0;

    public LayoutTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        name = getRequiredAttribute("name");
        mode = getRequiredAttribute("mode");
        value = getRequiredAttribute("value");
    }

    private void resetCounters() {
        rowCounter = 0;
        widgetCounter = 0;
    }

    /**
     * If resolved layout has a template, apply it, else apply widget type
     * handlers for widgets, ignoring rows.
     */
    // XXX same handler is used in different threads => synchronize it since
    // some member fields are not supposed to be shared
    public synchronized void apply(FaceletContext ctx, UIComponent parent)
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

        layout = layoutService.getLayout(ctx, layoutName, modeValue, valueName);
        if (layout == null) {
            log.error(String.format("Layout %s not found", layoutName));
            return;
        }

        // expose layout value
        ValueExpression layoutVe = eFactory.createValueExpression(layout,
                Layout.class);
        vm.setVariable(RenderVariables.layoutVariables.layout.name(), layoutVe);

        // set unique id on layout
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        layout.setId(helper.generateLayoutId(layout.getName()));
        String template = layout.getTemplate();
        if (template != null) {
            // XXX same handler is used in different threads => reset counters
            // before use
            resetCounters();
            ctx.pushClient(this);
            try {
                ctx.includeFacelet(parent, template);
            } finally {
                ctx.popClient(this);
                ctx.setVariableMapper(orig);
            }
        } else {

            for (LayoutRow row : layout.getRows()) {
                for (Widget widget : row.getWidgets()) {
                    WidgetTagHandler.applyWidgetHandler(ctx, parent, config,
                            widget, value, false);
                }
            }
            ctx.setVariableMapper(orig);
        }
    }

    /**
     * Exposes row and widget variables resolved from given name.
     * <p>
     * Works in conjunction with {@link LayoutRowTagHandler} and
     * {@link LayoutRowWidgetTagHandler}.
     * <p>
     * Row variables exposed: {@link RenderVariables.rowVariables#row} and
     * {@link RenderVariables.rowVariables#rowIndex}. Widget variables exposed:
     * {@link RenderVariables.widgetVariables#widget} and
     * {@link RenderVariables.widgetVariables#widgetIndex}.
     */
    public boolean apply(FaceletContext ctx, UIComponent parent, String name)
            throws IOException, FacesException, FaceletException, ELException {
        if (layout == null) {
            return false;
        }
        Integer rowNumber = TemplateClientHelper.getRowNumber(name);
        if (rowNumber != null) {
            LayoutRow[] rows = layout.getRows();
            if (rows == null || rowNumber > rows.length - 1) {
                return false;
            }
            LayoutRow row = rows[rowNumber];
            // expose row variables
            VariableMapper vm = ctx.getVariableMapper();
            ValueExpression rowVe = ctx.getExpressionFactory().createValueExpression(
                    row, LayoutRow.class);
            vm.setVariable(RenderVariables.rowVariables.layoutRow.name(), rowVe);
            ValueExpression rowIndexVe = ctx.getExpressionFactory().createValueExpression(
                    rowNumber, Integer.class);
            vm.setVariable(RenderVariables.rowVariables.layoutRowIndex.name(),
                    rowIndexVe);
            rowCounter = rowNumber;
            return true;
        }
        Integer widgetNumber = TemplateClientHelper.getWidgetNumber(name);
        if (widgetNumber != null) {
            LayoutRow[] rows = layout.getRows();
            if (rows == null || rowCounter > rows.length - 1) {
                return false;
            }
            Widget[] widgets = rows[rowCounter].getWidgets();
            if (widgets == null || widgetNumber > widgets.length - 1) {
                return false;
            }
            Widget widget = widgets[widgetNumber];
            // expose widget variables
            VariableMapper vm = ctx.getVariableMapper();
            ValueExpression widgetVe = ctx.getExpressionFactory().createValueExpression(
                    widget, Widget.class);
            vm.setVariable(RenderVariables.widgetVariables.widget.name(),
                    widgetVe);
            Integer level = null;
            if (widget != null) {
                level = widget.getLevel();
            }
            vm.setVariable(String.format("%s_%s",
                    RenderVariables.widgetVariables.widget.name(), level),
                    widgetVe);
            ValueExpression widgetIndexVe = ctx.getExpressionFactory().createValueExpression(
                    widgetNumber, Integer.class);
            vm.setVariable(RenderVariables.widgetVariables.widgetIndex.name(),
                    widgetIndexVe);
            vm.setVariable(String.format("%s_%s",
                    RenderVariables.widgetVariables.widgetIndex.name(), level),
                    widgetIndexVe);
            widgetCounter = widgetNumber;
            return true;
        }
        return false;
    }
}
