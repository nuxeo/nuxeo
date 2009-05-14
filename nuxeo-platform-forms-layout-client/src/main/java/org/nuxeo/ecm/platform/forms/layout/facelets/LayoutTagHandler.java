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
public class LayoutTagHandler extends TagHandler {

    private static final Log log = LogFactory.getLog(LayoutTagHandler.class);

    protected final TagConfig config;

    protected final TagAttribute name;

    protected final TagAttribute mode;

    protected final TagAttribute value;

    public LayoutTagHandler(TagConfig config) {
        super(config);
        this.config = config;
        name = getRequiredAttribute("name");
        mode = getRequiredAttribute("mode");
        value = getRequiredAttribute("value");
    }

    /**
     * If resolved layout has a template, apply it, else apply widget type
     * handlers for widgets, ignoring rows.
     */
    // TODO: add javadoc about variables exposed
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

        Layout layout = layoutService.getLayout(ctx, layoutName, modeValue, valueName);
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
        try {
            if (template != null) {
                ctx.includeFacelet(parent, template);
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
