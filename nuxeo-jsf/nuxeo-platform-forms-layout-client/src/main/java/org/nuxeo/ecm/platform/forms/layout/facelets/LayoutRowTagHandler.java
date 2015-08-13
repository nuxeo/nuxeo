/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LayoutRowTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.ui.web.binding.MetaVariableMapper;

/**
 * Layout row recursion tag handler.
 * <p>
 * Iterate over the layout rows and apply next handlers as many times as needed.
 * <p>
 * Only works when used inside a tag using the {@link LayoutTagHandler} template client.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutRowTagHandler extends TagHandler {

    private static final Log log = LogFactory.getLog(LayoutRowTagHandler.class);

    protected final TagConfig config;

    public LayoutRowTagHandler(TagConfig config) {
        super(config);
        this.config = config;
    }

    /**
     * For each row in layout, exposes row variables and applies next handler.
     * <p>
     * Needs layout to be exposed in context, so works in conjunction with {@link LayoutTagHandler}.
     * <p>
     * Row variables exposed: {@link RenderVariables.rowVariables#layoutRow} and
     * {@link RenderVariables.rowVariables#layoutRowIndex}, as well as
     * {@link RenderVariables.columnVariables#layoutColumn} and
     * {@link RenderVariables.columnVariables#layoutColumnIndex}, that act are aliases.
     */
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException {
        // resolve rows from layout in context
        Layout layout = null;
        String layoutVariableName = RenderVariables.layoutVariables.layout.name();
        FaceletHandlerHelper helper = new FaceletHandlerHelper(config);
        TagAttribute layoutAttribute = helper.createAttribute(layoutVariableName, "#{" + layoutVariableName + "}");
        if (layoutAttribute != null) {
            layout = (Layout) layoutAttribute.getObject(ctx, Layout.class);
        }
        if (layout == null) {
            log.error("Could not resolve layout " + layoutAttribute);
            return;
        }

        LayoutRow[] rows = layout.getRows();
        if (rows == null || rows.length == 0) {
            return;
        }

        VariableMapper orig = ctx.getVariableMapper();
        try {
            int rowCounter = 0;
            for (LayoutRow row : rows) {
                MetaVariableMapper vm = new MetaVariableMapper(orig);
                ctx.setVariableMapper(vm);

                // expose row variables
                ExpressionFactory eFactory = ctx.getExpressionFactory();
                ValueExpression rowVe = eFactory.createValueExpression(row, LayoutRow.class);
                vm.setVariable(RenderVariables.rowVariables.layoutRow.name(), rowVe);
                vm.addBlockedPattern(RenderVariables.rowVariables.layoutRow.name());
                vm.setVariable(RenderVariables.columnVariables.layoutColumn.name(), rowVe);
                vm.addBlockedPattern(RenderVariables.columnVariables.layoutColumn.name());
                ValueExpression rowIndexVe = eFactory.createValueExpression(rowCounter, Integer.class);
                vm.setVariable(RenderVariables.rowVariables.layoutRowIndex.name(), rowIndexVe);
                vm.addBlockedPattern(RenderVariables.rowVariables.layoutRowIndex.name());
                vm.setVariable(RenderVariables.columnVariables.layoutColumnIndex.name(), rowIndexVe);
                vm.addBlockedPattern(RenderVariables.columnVariables.layoutColumnIndex.name());

                nextHandler.apply(ctx, parent);
                rowCounter++;
            }
        } finally {
            ctx.setVariableMapper(orig);
        }
    }
}
