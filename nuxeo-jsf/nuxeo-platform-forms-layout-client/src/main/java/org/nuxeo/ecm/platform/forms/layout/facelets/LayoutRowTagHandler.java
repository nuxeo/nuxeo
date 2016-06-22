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
 * $Id: LayoutRowTagHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

import java.io.IOException;
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
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.ui.web.binding.BlockingVariableMapper;
import org.nuxeo.ecm.platform.ui.web.binding.MetaValueExpression;

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
        FaceletHandlerHelper helper = new FaceletHandlerHelper(ctx, config);

        if (FaceletHandlerHelper.isAliasOptimEnabled()) {
            applyOptimized(ctx, parent, helper);
        } else {
            applyCompat(ctx, parent, helper);
        }
    }

    protected void applyOptimized(FaceletContext ctx, UIComponent parent, FaceletHandlerHelper helper)
            throws IOException, FacesException, ELException {
        String rowCountVarName = RenderVariables.layoutVariables.layoutRowCount.name();
        TagAttribute rowCountAttr = helper.createAttribute(rowCountVarName, "#{" + rowCountVarName + "}");
        int rowCount = rowCountAttr.getInt(ctx);

        if (rowCount == 0) {
            return;
        }

        VariableMapper orig = ctx.getVariableMapper();
        try {
            for (int i = 0; i < rowCount; i++) {
                BlockingVariableMapper vm = new BlockingVariableMapper(orig);
                ctx.setVariableMapper(vm);
                // expose row variables
                ExpressionFactory eFactory = ctx.getExpressionFactory();
                ValueExpression ve = eFactory.createValueExpression(
                        "#{" + RenderVariables.layoutVariables.layout.name() + ".rows[" + i + "]}", String.class);
                ValueExpression rowVe = new MetaValueExpression(ve, ctx.getFunctionMapper(), vm, LayoutRow.class);
                ValueExpression rowIndexVe = eFactory.createValueExpression(i, Integer.class);
                String instanceName = getInstanceName();
                String indexName = getIndexName();
                vm.setVariable(instanceName, rowVe);
                vm.addBlockedPattern(instanceName);
                vm.setVariable(indexName, rowIndexVe);
                vm.addBlockedPattern(indexName);

                nextHandler.apply(ctx, parent);
            }
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

    protected void applyCompat(FaceletContext ctx, UIComponent parent, FaceletHandlerHelper helper)
            throws IOException, FacesException, ELException {
        // resolve rows from layout in context
        Layout layout = null;
        String layoutVariableName = RenderVariables.layoutVariables.layout.name();
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

        int rowCounter = 0;
        for (LayoutRow row : rows) {
            // expose row variables
            Map<String, ValueExpression> variables = new HashMap<String, ValueExpression>();
            ValueExpression rowVe = ctx.getExpressionFactory().createValueExpression(row, LayoutRow.class);
            variables.put(RenderVariables.rowVariables.layoutRow.name(), rowVe);
            variables.put(RenderVariables.columnVariables.layoutColumn.name(), rowVe);
            ValueExpression rowIndexVe = ctx.getExpressionFactory().createValueExpression(Integer.valueOf(rowCounter),
                    Integer.class);
            variables.put(RenderVariables.rowVariables.layoutRowIndex.name(), rowIndexVe);
            variables.put(RenderVariables.columnVariables.layoutColumnIndex.name(), rowIndexVe);

            List<String> blockedPatterns = new ArrayList<String>();
            blockedPatterns.add(RenderVariables.rowVariables.layoutRow.name());
            blockedPatterns.add(RenderVariables.rowVariables.layoutRowIndex.name());
            blockedPatterns.add(RenderVariables.columnVariables.layoutColumn.name());
            blockedPatterns.add(RenderVariables.columnVariables.layoutColumnIndex.name());

            FaceletHandler handler = helper.getAliasTagHandler(row.getTagConfigId(), variables, blockedPatterns,
                    nextHandler);
            handler.apply(ctx, parent);
            rowCounter++;
        }
    }

    protected String getInstanceName() {
        return RenderVariables.rowVariables.layoutRow.name();
    }

    protected String getIndexName() {
        return RenderVariables.rowVariables.layoutRowIndex.name();
    }

}
