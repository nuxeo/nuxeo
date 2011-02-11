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
 * $Id: SubWidgetTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
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
import org.nuxeo.ecm.platform.forms.layout.api.Widget;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * SubWidget tag handler.
 * <p>
 * Iterates over a widget subwidgets and apply next handlers as many times as
 * needed.
 * <p>
 * Only works when used inside a tag using the {@link WidgetTagHandler}.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class SubWidgetTagHandler extends TagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(SubWidgetTagHandler.class);

    protected final TagConfig config;

    public SubWidgetTagHandler(TagConfig config) {
        super(config);
        this.config = config;
    }

    /**
     * For each subwidget in current widget, exposes widget variables and
     * applies next handler.
     * <p>
     * Needs widget to be exposed in context, so works in conjunction with
     * {@link WidgetTagHandler}.
     * <p>
     * Widget variables exposed: {@link RenderVariables.widgetVariables#widget},
     * same variable suffixed with "_n" where n is the widget level, and
     * {@link RenderVariables.widgetVariables#widgetIndex}.
     */
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, ELException {
        // resolve subwidgets from widget in context
        Widget widget = null;
        String widgetVariableName = RenderVariables.widgetVariables.widget.name();
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        TagAttribute widgetAttribute = helper.createAttribute(
                widgetVariableName, String.format("#{%s}", widgetVariableName));
        if (widgetAttribute != null) {
            widget = (Widget) widgetAttribute.getObject(ctx, Widget.class);
        }
        if (widget == null) {
            log.error("Could not resolve widget " + widgetAttribute);
            return;
        }

        Widget[] subWidgets = widget.getSubWidgets();
        if (subWidgets == null || subWidgets.length == 0) {
            return;
        }

        int subWidgetCounter = 0;
        for (Widget subWidget : subWidgets) {
            // expose widget variables
            Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
            ValueExpression subWidgetVe = ctx.getExpressionFactory().createValueExpression(
                    subWidget, Widget.class);
            variables.put(RenderVariables.widgetVariables.widget.name(),
                    subWidgetVe);
            variables.put(String.format("%s_%s",
                    RenderVariables.widgetVariables.widget.name(),
                    subWidget.getLevel()), subWidgetVe);
            ValueExpression subWidgetIndexVe = ctx.getExpressionFactory().createValueExpression(
                    subWidgetCounter, Integer.class);
            variables.put(RenderVariables.widgetVariables.widgetIndex.name(),
                    subWidgetIndexVe);
            variables.put(String.format("%s_%s",
                    RenderVariables.widgetVariables.widgetIndex.name(),
                    subWidget.getLevel()), subWidgetIndexVe);

            FaceletHandler handlerWithVars = helper.getAliasTagHandler(
                    variables, nextHandler);
            // apply
            handlerWithVars.apply(ctx, parent);
            subWidgetCounter++;
        }
    }
}
