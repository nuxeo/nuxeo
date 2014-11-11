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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
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
 */
public class WidgetTagHandler extends MetaTagHandler {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(WidgetTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute widget;

    // NXP-4412: not possible right now to take into account specified mode =>
    // add back later
    // protected TagAttribute mode;

    protected final TagAttribute value;

    protected final TagAttribute[] vars;

    protected final String[] reservedVarsArray = { "id", "widget", "value" };

    public WidgetTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        widget = getRequiredAttribute("widget");
        value = getRequiredAttribute("value");
        vars = tag.getAttributes().getAll();
    }

    /**
     * Renders given widget resolving its {@link FaceletHandler} from
     * {@link WebLayoutManager} configuration.
     * <p>
     * Variables exposed: {@link RenderVariables.globalVariables#value}, same
     * variable suffixed with "_n" where n is the widget level, and
     * {@link RenderVariables.globalVariables#document}.
     */
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, ELException {
        // build handler
        Widget widgetInstance = (Widget) widget.getObject(ctx, Widget.class);
        if (widgetInstance != null) {
            // set value name on widget instance in case it's changed from
            // first computation
            String valueName = value.getValue();
            if (ComponentTagUtils.isValueReference(valueName)) {
                valueName = valueName.substring(2, valueName.length() - 1);
            }
            // add additional properties put on tag
            List<String> reservedVars = Arrays.asList(reservedVarsArray);
            for (TagAttribute var : vars) {
                String localName = var.getLocalName();
                if (!reservedVars.contains(localName)) {
                    widgetInstance.setProperty(localName, var.getValue());
                }
            }
            widgetInstance.setValueName(valueName);
            applyWidgetHandler(ctx, parent, config, widgetInstance, value, true);
        }
    }

    public static void generateWidgetIdsRecursive(FaceletHandlerHelper helper,
            Widget widget) {
        if (widget == null) {
            return;
        }
        widget.setId(helper.generateWidgetId(widget.getName()));
        Widget[] subWidgets = widget.getSubWidgets();
        if (subWidgets != null) {
            for (Widget subWidget : subWidgets) {
                generateWidgetIdsRecursive(helper, subWidget);
            }
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
        generateWidgetIdsRecursive(helper, widget);

        FaceletHandler handler = layoutService.getFaceletHandler(ctx, config,
                widget);
        if (handler == null) {
            return;
        }
        if (fillVariables) {
            // expose widget variables
            Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();

            ValueExpression valueExpr = value.getValueExpression(ctx,
                    Object.class);
            variables.put(RenderVariables.globalVariables.value.name(),
                    valueExpr);
            variables.put(String.format("%s_%s",
                    RenderVariables.globalVariables.value.name(),
                    widget.getLevel()), valueExpr);
            // document as alias to value
            variables.put(RenderVariables.globalVariables.document.name(),
                    valueExpr);

            FaceletHandler handlerWithVars = helper.getAliasTagHandler(
                    variables, handler);
            // apply
            handlerWithVars.apply(ctx, parent);

        } else {
            // just apply
            handler.apply(ctx, parent);
        }
    }

}
