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
import java.io.Serializable;
import java.util.ArrayList;
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
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.ui.web.binding.BlockingVariableMapper;

/**
 * SubWidget tag handler.
 * <p>
 * Iterates over a widget subwidgets and apply next handlers as many times as needed.
 * <p>
 * Only works when used inside a tag using the {@link WidgetTagHandler}.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class SubWidgetTagHandler extends TagHandler {

    private static final Log log = LogFactory.getLog(SubWidgetTagHandler.class);

    protected final TagConfig config;

    /**
     * @since 7.2
     */
    protected final TagAttribute recomputeIds;

    public SubWidgetTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        recomputeIds = getAttribute("recomputeIds");
    }

    /**
     * For each subwidget in current widget, exposes widget variables and applies next handler.
     * <p>
     * Needs widget to be exposed in context, so works in conjunction with {@link WidgetTagHandler}.
     * <p>
     * Widget variables exposed: {@link RenderVariables.widgetVariables#widget} , same variable suffixed with "_n" where
     * n is the widget level, and {@link RenderVariables.widgetVariables#widgetIndex}.
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException {
        // resolve subwidgets from widget in context
        Widget widget = null;
        String widgetVariableName = RenderVariables.widgetVariables.widget.name();
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        TagAttribute widgetAttribute = helper.createAttribute(widgetVariableName, "#{" + widgetVariableName + "}");
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

        boolean recomputeIdsBool = false;
        if (recomputeIds != null) {
            recomputeIdsBool = recomputeIds.getBoolean(ctx);
        }

        if (FaceletHandlerHelper.isAliasOptimEnabled()) {
            VariableMapper orig = ctx.getVariableMapper();
            try {
                applyOptimized(ctx, parent, orig, subWidgets, helper, recomputeIdsBool);
            } finally {
                ctx.setVariableMapper(orig);
            }
        } else {
            applyCompat(ctx, parent, subWidgets, helper, recomputeIdsBool);
        }
    }

    protected void applyOptimized(FaceletContext ctx, UIComponent parent, VariableMapper orig, Widget[] subWidgets,
            FaceletHandlerHelper helper, boolean recomputeIdsBool) throws IOException, FacesException, ELException {
        int subWidgetCounter = 0;
        for (Widget subWidget : subWidgets) {
            BlockingVariableMapper vm = new BlockingVariableMapper(orig);
            ctx.setVariableMapper(vm);

            // set unique id on widget before exposing it to the context, but assumes iteration could be done
            // several times => do not generate id again if already set, unless specified by attribute
            // "recomputeIds"
            if (subWidget != null && (subWidget.getId() == null || recomputeIdsBool)) {
                WidgetTagHandler.generateWidgetId(ctx, helper, subWidget, false);
            }

            WidgetTagHandler.exposeWidgetVariables(ctx, vm, subWidget, subWidgetCounter, true);

            nextHandler.apply(ctx, parent);
            subWidgetCounter++;
        }
    }

    protected void applyCompat(FaceletContext ctx, UIComponent parent, Widget[] subWidgets, FaceletHandlerHelper helper,
            boolean recomputeIdsBool) throws IOException, FacesException, ELException {
        int subWidgetCounter = 0;
        for (Widget subWidget : subWidgets) {
            // set unique id on widget before exposing it to the context, but assumes iteration could be done several
            // times => do not generate id again if already set, unless specified by attribute "recomputeIds"
            if (subWidget != null && (subWidget.getId() == null || recomputeIdsBool)) {
                WidgetTagHandler.generateWidgetId(helper, subWidget, false);
            }

            // expose widget variables
            Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
            ExpressionFactory eFactory = ctx.getExpressionFactory();
            ValueExpression subWidgetVe = eFactory.createValueExpression(subWidget, Widget.class);
            Integer level = null;
            String tagConfigId = null;
            if (subWidget != null) {
                level = Integer.valueOf(subWidget.getLevel());
                tagConfigId = subWidget.getTagConfigId();
            }

            variables.put(RenderVariables.widgetVariables.widget.name(), subWidgetVe);
            // variables.put(String.format("%s_%s",
            // RenderVariables.widgetVariables.widget.name(), level),
            // subWidgetVe);
            ValueExpression subWidgetIndexVe = eFactory.createValueExpression(Integer.valueOf(subWidgetCounter),
                    Integer.class);
            variables.put(RenderVariables.widgetVariables.widgetIndex.name(), subWidgetIndexVe);
            variables.put(RenderVariables.widgetVariables.widgetIndex.name() + "_" + level, subWidgetIndexVe);

            // XXX: expose widget controls too, need to figure out
            // why controls cannot be references to widget.controls like
            // properties are in TemplateWidgetTypeHandler
            if (subWidget != null) {
                for (Map.Entry<String, Serializable> ctrl : subWidget.getControls().entrySet()) {
                    String key = ctrl.getKey();
                    String name = RenderVariables.widgetVariables.widgetControl.name() + "_" + key;
                    Serializable value = ctrl.getValue();
                    variables.put(name, eFactory.createValueExpression(value, Object.class));
                }
            }

            List<String> blockedPatterns = new ArrayList<String>();
            blockedPatterns.add(RenderVariables.widgetVariables.widget.name());
            blockedPatterns.add(RenderVariables.widgetVariables.widgetIndex.name() + "*");
            blockedPatterns.add(RenderVariables.widgetVariables.widgetControl.name() + "_*");

            FaceletHandler handlerWithVars = helper.getAliasTagHandler(tagConfigId, variables, blockedPatterns,
                    nextHandler);

            // apply
            handlerWithVars.apply(ctx, parent);
            subWidgetCounter++;
        }
    }

}
