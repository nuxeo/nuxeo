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

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletHandler;
import com.sun.facelets.TemplateClient;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.MetaTagHandler;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;

/**
 * Widget tag handler.
 * <p>
 * Applies {@link  WidgetTypeHandler} found for given widget, in given mode and
 * for given value.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class WidgetTagHandler extends MetaTagHandler implements TemplateClient {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WidgetTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute widget;

    protected final TagAttribute mode;

    protected final TagAttribute value;

    // composition attribute

    // XXX AT: maybe not keeping this value as a tag instance would avoid the
    // apply method to be synchronized
    protected Widget widgetInstance = null;

    public WidgetTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        widget = getRequiredAttribute("widget");
        mode = getRequiredAttribute("mode");
        value = getRequiredAttribute("value");
    }

    // XXX same handler is used in different threads => synchronize it since
    // some member fields are not supposed to be shared
    public synchronized void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, ELException {
        // build handler
        widgetInstance = (Widget) widget.getObject(ctx, Widget.class);
        if (widgetInstance != null) {
            // set value name on widget instance in case it's changed from first
            // computation
            String valueName = value.getValue();
            if (ComponentTagUtils.isValueReference(valueName)) {
                valueName = valueName.substring(2, valueName.length() - 1);
            }
            widgetInstance.setValueName(valueName);
            ctx.extendClient(this);
            applyWidgetHandler(ctx, parent, config, widgetInstance, value, true);
            ctx.popClient(this);
        }
    }

    public static void applyWidgetHandler(FaceletContext ctx,
            UIComponent parent, TagConfig config, Widget widget,
            TagAttribute value, boolean fillVariables) throws IOException {
        if (widget == null) {
            return;
        }
        WebLayoutManager layoutService;
        try {
            layoutService = Framework.getService(WebLayoutManager.class);
        } catch (Exception e) {
            throw new FacesException(e);
        }

        // set unique id on widget and sub widgets before building handler.
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);
        widget.setId(helper.generateWidgetId(widget.getName()));
        Widget[] subWidgets = widget.getSubWidgets();
        if (subWidgets != null) {
            for (Widget subWidget : subWidgets) {
                if (subWidget != null) {
                    subWidget.setId(helper.generateWidgetId(subWidget.getName()));
                }
            }
        }

        // log.debug("thread=" + String.valueOf(Thread.currentThread().getId())
        // + ", handler=" + tHandler.hashCode() + ", w=" + widget.getId());

        FaceletHandler handler = layoutService.getFaceletHandler(ctx, config,
                widget);
        if (handler == null) {
            return;
        }
        if (fillVariables) {
            // expose widget variables to the variable mapper
            VariableMapper orig = ctx.getVariableMapper();
            VariableMapper vm = new VariableMapperWrapper(orig);
            ctx.setVariableMapper(vm);
            ValueExpression valueExpr = value.getValueExpression(ctx,
                    Object.class);
            vm.setVariable(RenderVariables.globalVariables.value.name(),
                    valueExpr);
            // document as alias to value
            vm.setVariable(RenderVariables.globalVariables.document.name(),
                    valueExpr);
            // apply
            handler.apply(ctx, parent);
            ctx.setVariableMapper(orig);
        } else {
            // just apply
            handler.apply(ctx, parent);
        }
    }

    public boolean apply(FaceletContext ctx, UIComponent parent, String name)
            throws IOException, FacesException, ELException {
        if (widgetInstance == null) {
            return false;
        }
        Integer subWidgetNumber = TemplateClientHelper.getSubWidgetNumber(name);
        if (subWidgetNumber != null) {
            Widget[] subWidgets = widgetInstance.getSubWidgets();
            if (subWidgets == null || subWidgetNumber > subWidgets.length - 1) {
                return false;
            }
            Widget subWidget = subWidgets[subWidgetNumber];
            // expose widget variables
            VariableMapper vm = ctx.getVariableMapper();
            ValueExpression subWidgetVe = ctx.getExpressionFactory().createValueExpression(
                    subWidget, Widget.class);
            vm.setVariable(RenderVariables.subWidgetVariables.subWidget.name(),
                    subWidgetVe);
            ValueExpression subWidgetIndexVe = ctx.getExpressionFactory().createValueExpression(
                    subWidgetNumber, Integer.class);
            vm.setVariable(
                    RenderVariables.subWidgetVariables.subWidgetIndex.name(),
                    subWidgetIndexVe);
            return true;
        }
        return false;
    }

}
