/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.model.ArrayDataModel;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.ResultSetDataModel;
import javax.faces.model.ScalarDataModel;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagAttributeException;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;

/**
 * Repeat handler.
 * <p>
 * This handler is different from the standard one because it performs a real
 * iteration on the tree structure, instead of keeping the same components and
 * saving their state in each of the iteration.
 * <p>
 * A real iteration is the only viable solution when using variables that will
 * have an impact on the tree structure, like a layout name and mode.
 *
 * @author Anahide Tchertchian
 */
public class RepeatTagHandler extends TagHandler {

    protected final TagAttribute value;

    protected final TagAttribute var;

    protected final TagAttribute index;

    // TODO: add size and offset

    public RepeatTagHandler(TagConfig config) {
        super(config);
        value = getAttribute("value");
        var = getAttribute("var");
        if (var != null && !var.isLiteral()) {
            throw new TagAttributeException(tag, var, "Must be literal");
        }
        index = getAttribute("index");
        if (index != null && !index.isLiteral()) {
            throw new TagAttributeException(tag, index, "Must be literal");
        }
    }

    private static final DataModel EMPTY_MODEL = new ListDataModel(
            Collections.emptyList());

    private DataModel getDataModel(FaceletContext ctx) {
        DataModel model;
        if (value == null) {
            model = EMPTY_MODEL;
        } else {
            Object val = value.getObject(ctx);
            if (val == null) {
                model = EMPTY_MODEL;
            } else if (val instanceof DataModel) {
                model = (DataModel) val;
            } else if (val instanceof List) {
                model = new ListDataModel((List) val);
            } else if (Object[].class.isAssignableFrom(val.getClass())) {
                model = new ArrayDataModel((Object[]) val);
            } else if (val instanceof ResultSet) {
                model = new ResultSetDataModel((ResultSet) val);
            } else {
                model = new ScalarDataModel(val);
            }
        }
        return model;
    }

    public synchronized void apply(FaceletContext ctx, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        DataModel dm = getDataModel(ctx);

        if (dm.getRowCount() < 1) {
            return;
        }

        VariableMapper orig = ctx.getVariableMapper();
        VariableMapper vm = new VariableMapperWrapper(orig);
        ctx.setVariableMapper(vm);
        try {
            ExpressionFactory ef = ctx.getExpressionFactory();
            String varValue = null;
            if (var != null) {
                varValue = var.getValue();
            }
            String indexValue = null;
            if (index != null) {
                indexValue = index.getValue();
            }
            dm.setRowIndex(-1);
            for (int i = 0; i < dm.getRowCount(); i++) {
                dm.setRowIndex(i);
                // expose variables
                if (varValue != null) {
                    ValueExpression varVe = ef.createValueExpression(
                            dm.getRowData(), Object.class);
                    vm.setVariable(varValue, varVe);
                }
                if (indexValue != null) {
                    ValueExpression indexVe = ef.createValueExpression(i,
                            Integer.class);
                    vm.setVariable(indexValue, indexVe);
                }
                nextHandler.apply(ctx, parent);
            }
        } finally {
            dm.setRowIndex(-1);
            ctx.setVariableMapper(orig);
        }
    }
}
