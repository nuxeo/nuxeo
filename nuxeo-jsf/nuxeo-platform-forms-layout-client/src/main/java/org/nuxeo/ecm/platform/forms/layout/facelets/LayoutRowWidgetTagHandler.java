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
 * $Id: LayoutRowWidgetTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.ui.web.binding.MetaValueExpression;
import org.nuxeo.ecm.platform.ui.web.binding.MetaVariableMapper;

/**
 * Layout widget recursion tag handler.
 * <p>
 * Iterates over a layout row widgets and apply next handlers as many times as needed.
 * <p>
 * Only works when used inside a tag using the {@link LayoutRowTagHandler}.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutRowWidgetTagHandler extends TagHandler {

    private static final Log log = LogFactory.getLog(LayoutRowWidgetTagHandler.class);

    protected final TagConfig config;

    /**
     * @since 7.2
     */
    protected final TagAttribute recomputeIds;

    public LayoutRowWidgetTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        recomputeIds = getAttribute("recomputeIds");
    }

    /**
     * For each widget in current row, exposes widget variables and applies next handler.
     * <p>
     * Needs row to be exposed in context, so works in conjunction with {@link LayoutRowTagHandler}.
     * <p>
     * Widget variables exposed: {@link RenderVariables.widgetVariables#widget} , same variable suffixed with "_n" where
     * n is the widget level, and {@link RenderVariables.widgetVariables#widgetIndex}.
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException,
            ELException {

        // resolve widgets from row in context
        LayoutRow row = null;
        String rowVariableName = RenderVariables.rowVariables.layoutRow.name();
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        TagAttribute rowAttribute = helper.createAttribute(rowVariableName, String.format("#{%s}", rowVariableName));
        if (rowAttribute != null) {
            row = (LayoutRow) rowAttribute.getObject(ctx, LayoutRow.class);
        }
        if (row == null) {
            log.error("Could not resolve layout row " + rowAttribute);
            return;
        }

        Widget[] widgets = row.getWidgets();
        if (widgets == null || widgets.length == 0) {
            return;
        }

        boolean recomputeIdsBool = false;
        if (recomputeIds != null) {
            recomputeIdsBool = recomputeIds.getBoolean(ctx);
        }

        VariableMapper orig = ctx.getVariableMapper();
        try {
            int widgetCounter = 0;
            for (Widget widget : widgets) {
                // set unique id on widget before exposing it to the context, but assumes iteration could be done
                // several
                // times => do not generate id again if already set, unless specified by attribute "recomputeIds"
                if (widget != null && (widget.getId() == null || recomputeIdsBool)) {
                    WidgetTagHandler.generateWidgetId(helper, widget, false);
                }

                MetaVariableMapper vm = new MetaVariableMapper(orig);
                ctx.setVariableMapper(vm);

                // expose widget variables
                ExpressionFactory eFactory = ctx.getExpressionFactory();
                ValueExpression widgetVe = eFactory.createValueExpression(widget, Widget.class);
                vm.setVariable(RenderVariables.widgetVariables.widget.name(), widgetVe);
                Integer level = null;
                if (widget != null) {
                    level = Integer.valueOf(widget.getLevel());
                }
                vm.setVariable(RenderVariables.widgetVariables.widget.name() + "_" + level, widgetVe);
                vm.addBlockedPattern(RenderVariables.widgetVariables.widget.name() + "*");
                ValueExpression widgetIndexVe = eFactory.createValueExpression(Integer.valueOf(widgetCounter),
                        Integer.class);
                vm.setVariable(RenderVariables.widgetVariables.widgetIndex.name(), widgetIndexVe);
                vm.setVariable(RenderVariables.widgetVariables.widgetIndex.name() + "_" + level, widgetIndexVe);
                vm.addBlockedPattern(RenderVariables.widgetVariables.widgetIndex.name() + "*");

                // expose widget controls too
                if (widget != null) {
                    for (Map.Entry<String, Serializable> ctrl : widget.getControls().entrySet()) {
                        String key = ctrl.getKey();
                        String name = RenderVariables.widgetVariables.widgetControl.name() + "_" + key;
                        String value = "#{" + RenderVariables.widgetVariables.widget.name() + ".controls." + key + "}";
                        ValueExpression ve = eFactory.createValueExpression(ctx, value, Object.class);
                        vm.setVariable(name, new MetaValueExpression(ve, ctx.getFunctionMapper(), vm));
                    }
                    vm.addBlockedPattern(RenderVariables.widgetVariables.widgetControl.name() + "_*");
                }

                // apply
                nextHandler.apply(ctx, parent);
                widgetCounter++;
            }
        } finally {
            ctx.setVariableMapper(orig);
        }
    }
}
