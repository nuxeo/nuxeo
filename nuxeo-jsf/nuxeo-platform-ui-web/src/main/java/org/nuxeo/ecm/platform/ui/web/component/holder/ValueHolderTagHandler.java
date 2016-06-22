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
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagAttribute;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasVariableMapper;
import org.nuxeo.ecm.platform.ui.web.tag.handler.GenericHtmlComponentHandler;
import org.nuxeo.ecm.platform.ui.web.util.FaceletDebugTracer;

import com.sun.faces.facelets.tag.jsf.ComponentSupport;

/**
 * Tag handler for a {@link UIValueHolder} component, that exposes the value kept by the component at build time for
 * children components.
 *
 * @since 5.5
 */
public class ValueHolderTagHandler extends GenericHtmlComponentHandler {

    protected final Log log = LogFactory.getLog(ValueHolderTagHandler.class);

    protected final TagAttribute var;

    /**
     * @since 8.2
     */
    protected final TagAttribute skip;

    public ValueHolderTagHandler(ComponentConfig config) {
        super(config);
        var = getAttribute("var");
        skip = getAttribute("skip");
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent) throws IOException {
        boolean skipValue = false;
        if (skip != null) {
            skipValue = skip.getBoolean(ctx);
        }
        if (skipValue) {
            super.applyNextHandler(ctx, parent);
        } else {
            super.apply(ctx, parent);
        }
    }

    @Override
    public void applyNextHandler(FaceletContext ctx, UIComponent c) throws IOException, FacesException, ELException {
        long start = FaceletDebugTracer.start();
        String varName = null;
        try {
            boolean varSet = false;
            if (var != null) {
                varName = var.getValue(ctx);
            }

            VariableMapper orig = ctx.getVariableMapper();
            AliasVariableMapper alias = new AliasVariableMapper();
            // XXX: reuse the component id as the alias variable mapper id so that
            // the value holder JSF component can reuse it at render time to expose
            // the value it keeps
            String aliasId = (String) c.getAttributes().get(ComponentSupport.MARK_CREATED);
            alias.setId(aliasId);

            if (!StringUtils.isBlank(varName)) {
                varSet = true;
                List<String> blockedPatterns = new ArrayList<String>();
                blockedPatterns.add(varName);
                alias.setBlockedPatterns(blockedPatterns);
            }

            try {
                if (varSet) {
                    Object valueToExpose = retrieveValueToExpose(ctx, c);
                    ExpressionFactory eFactory = ctx.getExpressionFactory();
                    ValueExpression valueVe = eFactory.createValueExpression(valueToExpose, Object.class);
                    alias.setVariable(varName, valueVe);
                    VariableMapper vm = alias.getVariableMapperForBuild(orig);
                    ctx.setVariableMapper(vm);
                    AliasVariableMapper.exposeAliasesToRequest(ctx.getFacesContext(), alias);
                }
                super.applyNextHandler(ctx, c);
            } finally {
                if (varSet) {
                    AliasVariableMapper.removeAliasesExposedToRequest(ctx.getFacesContext(), aliasId);
                    ctx.setVariableMapper(orig);
                }
            }
        } finally {
            FaceletDebugTracer.trace(start, getTag(), varName);
        }
    }

    /**
     * Returns the value to expose at build time for this tag handler.
     * <p>
     * Value can be retrieved directly from component in most of cases, but should be retrieved from view-scoped managed
     * bean when the restore phase is called (as component has not been restored yet, so its value is not available to
     * be exposed in the tree view being built).
     *
     * @since 6.0
     */
    protected Object retrieveValueToExpose(FaceletContext context, UIComponent comp) {
        if (comp instanceof UIValueHolder) {
            UIValueHolder c = (UIValueHolder) comp;
            FacesContext faces = context.getFacesContext();
            if (PhaseId.RESTORE_VIEW.equals(faces.getCurrentPhaseId())) {
                // lookup backing bean
                NuxeoValueHolderBean bean = c.lookupBean(faces);
                if (bean != null) {
                    String fid = c.getFaceletId();
                    if (fid != null && bean.hasState(fid)) {
                        return bean.getState(fid);
                    }
                }
            }
            return c.getValueToExpose();
        } else {
            String className = null;
            if (comp != null) {
                className = comp.getClass().getName();
            }
            log.error("Associated component with class '" + className
                    + "' is not a UIValueHolder instance => cannot retrieve value to expose.");
        }
        return null;
    }

}
