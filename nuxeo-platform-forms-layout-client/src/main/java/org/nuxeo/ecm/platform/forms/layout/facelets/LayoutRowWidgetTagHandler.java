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
import java.util.HashMap;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * Layout widget recursion tag handler.
 * <p>
 * Iterates over a layout row widgets and apply next handlers as many times as
 * needed.
 * <p>
 * Only works when used inside a tag using the {@link LayoutRowTagHandler}.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutRowWidgetTagHandler extends TagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(LayoutRowWidgetTagHandler.class);

    protected final TagConfig config;

    public LayoutRowWidgetTagHandler(TagConfig config) {
        super(config);
        this.config = config;
    }

    /**
     * For each widget in current row, exposes widget variables and applies
     * next handler.
     * <p>
     * Needs row to be exposed in context, so works in conjunction with
     * {@link LayoutRowTagHandler}.
     * <p>
     * Widget variables exposed: {@link RenderVariables.widgetVariables#widget},
     * same variable suffixed with "_n" where n is the widget level, and
     * {@link RenderVariables.widgetVariables#widgetIndex}.
     */
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {

        // resolve widgets from row in context
        LayoutRow row = null;
        String rowVariableName = RenderVariables.rowVariables.layoutRow.name();
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        TagAttribute rowAttribute = helper.createAttribute(rowVariableName,
                String.format("#{%s}", rowVariableName));
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

        int widgetCounter = 0;
        for (Widget widget : widgets) {
            // expose widget variables
            Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
            ValueExpression widgetVe = ctx.getExpressionFactory().createValueExpression(
                    widget, Widget.class);
            variables.put(RenderVariables.widgetVariables.widget.name(),
                    widgetVe);
            Integer level = null;
            if (widget != null) {
                level = widget.getLevel();
            }
            variables.put(String.format("%s_%s",
                    RenderVariables.widgetVariables.widget.name(), level),
                    widgetVe);
            ValueExpression widgetIndexVe = ctx.getExpressionFactory().createValueExpression(
                    widgetCounter, Integer.class);
            variables.put(RenderVariables.widgetVariables.widgetIndex.name(),
                    widgetIndexVe);
            variables.put(String.format("%s_%s",
                    RenderVariables.widgetVariables.widgetIndex.name(), level),
                    widgetIndexVe);

            FaceletHandler handler = helper.getAliasTagHandler(variables,
                    nextHandler);

            // apply
            handler.apply(ctx, parent);
            widgetCounter++;
        }
    }
}
