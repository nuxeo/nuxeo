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
import java.util.ArrayList;
import java.util.List;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasVariableMapper;
import org.nuxeo.ecm.platform.ui.web.tag.handler.GenericHtmlComponentHandler;

import com.sun.faces.facelets.tag.jsf.ComponentSupport;

/**
 * Tag handler for a {@link UIValueHolder} component, that exposes the value
 * kept by the component at build time for children components.
 *
 * @since 5.5
 */
public class ValueHolderTagHandler extends GenericHtmlComponentHandler {

    protected final Log log = LogFactory.getLog(ValueHolderTagHandler.class);

    protected final TagAttribute var;

    public ValueHolderTagHandler(ComponentConfig config) {
        super(config);
        var = getAttribute("var");
    }

    @Override
    public void applyNextHandler(FaceletContext ctx, UIComponent c)
            throws IOException, FacesException, ELException {

        String varName = null;
        boolean varSet = false;
        if (var != null) {
            varName = var.getValue(ctx);
        }

        VariableMapper orig = ctx.getVariableMapper();
        AliasVariableMapper alias = new AliasVariableMapper();
        // XXX: reuse the component id as the alias variable mapper id so that
        // the value holder JSF component can reuse it at render time to expose
        // the value it keeps
        String aliasId = (String) c.getAttributes().get(
                ComponentSupport.MARK_CREATED);
        alias.setId(aliasId);

        if (!StringUtils.isBlank(varName)) {
            varSet = true;
            List<String> blockedPatterns = new ArrayList<String>();
            blockedPatterns.add(varName);
            alias.setBlockedPatterns(blockedPatterns);
        }

        try {
            if (varSet) {
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
                alias.setVariable(varName, valueVe);
                VariableMapper vm = alias.getVariableMapperForBuild(orig);
                ctx.setVariableMapper(vm);
                AliasVariableMapper.exposeAliasesToRequest(
                        ctx.getFacesContext(), alias);
            }
            super.applyNextHandler(ctx, c);
        } finally {
            if (varSet) {
                AliasVariableMapper.removeAliasesExposedToRequest(
                        ctx.getFacesContext(), aliasId);
                ctx.setVariableMapper(orig);
            }
        }
    }

}
