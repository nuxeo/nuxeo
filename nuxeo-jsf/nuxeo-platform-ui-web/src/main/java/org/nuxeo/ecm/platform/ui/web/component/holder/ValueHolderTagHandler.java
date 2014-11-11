/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.holder;

import java.io.IOException;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.component.VariableManager;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.jsf.ComponentConfig;
import com.sun.facelets.tag.jsf.html.HtmlComponentHandler;

/**
 * Tag handler for a {@link UIValueHolder} component, that exposes the value
 * kept by the component at build time for children components.
 *
 * @since 5.5
 */
public class ValueHolderTagHandler extends HtmlComponentHandler {

    protected final Log log = LogFactory.getLog(ValueHolderTagHandler.class);

    protected final TagAttribute var;

    public ValueHolderTagHandler(ComponentConfig config) {
        super(config);
        var = getAttribute("var");
    }

    @Override
    protected void applyNextHandler(FaceletContext ctx, UIComponent c)
            throws IOException, FacesException, ELException {
        VariableMapper orig = ctx.getVariableMapper();
        Object origVarValue = null;

        String varName = null;
        boolean varSet = false;
        if (var != null) {
            varName = var.getValue(ctx);
        }
        if (!StringUtils.isBlank(varName)) {
            varSet = true;
            origVarValue = VariableManager.saveRequestMapVarValue(varName);
        }
        try {
            if (varSet) {
                VariableMapper vm = new VariableMapperWrapper(orig);
                ctx.setVariableMapper(vm);
                Object valueToExpose = null;
                if (c instanceof UIValueHolder) {
                    UIValueHolder holder = (UIValueHolder) c;
                    valueToExpose = holder.getValueToExpose();
                } else {
                    String className = null;
                    if (c != null) {
                        className = c.getClass().getName();
                    }
                    log.error(String.format(
                            "Associated component with class '%s' is not"
                                    + " a UIValueHolder instance => cannot "
                                    + "retrieve value to expose.", className));
                }
                ExpressionFactory eFactory = ctx.getExpressionFactory();
                ValueExpression valueVe = eFactory.createValueExpression(
                        valueToExpose, Object.class);
                vm.setVariable(varName, valueVe);
                VariableManager.putVariableToRequestParam(varName,
                        valueToExpose);
            }
            super.applyNextHandler(ctx, c);
        } finally {
            if (varSet) {
                VariableManager.restoreRequestMapVarValue(varName, origVarValue);
                ctx.setVariableMapper(orig);
            }
        }
    }

}
