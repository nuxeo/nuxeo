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
import java.util.List;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasEvent;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasVariableMapper;
import org.nuxeo.ecm.platform.ui.web.component.ResettableComponent;

import com.sun.faces.facelets.tag.jsf.ComponentSupport;

/**
 * Component that keeps and exposes a value to the context during each JSF phase.
 * <p>
 * Can be bound to a value as an input component, or not submit the value and still expose it to the context at build
 * time as well as at render time.
 *
 * @since 5.5
 */
public class UIValueHolder extends HtmlInputText implements ResettableComponent {

    private static final Log log = LogFactory.getLog(UIValueHolder.class);

    public static final String COMPONENT_TYPE = UIValueHolder.class.getName();

    public static final String COMPONENT_FAMILY = UIInput.COMPONENT_FAMILY;

    protected String var;

    /**
     * <p>
     * The submittedValue value of this {@link UIInput} component.
     * </p>
     */
    protected transient Object submittedValue = null;

    protected Boolean submitValue;

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public String getRendererType() {
        return COMPONENT_TYPE;
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void broadcast(FacesEvent event) {
        if (event instanceof AliasEvent) {
            FacesContext context = getFacesContext();
            AliasVariableMapper alias = getAliasVariableMapper(context);
            try {
                AliasVariableMapper.exposeAliasesToRequest(context, alias);
                FacesEvent origEvent = ((AliasEvent) event).getOriginalEvent();
                origEvent.getComponent().broadcast(origEvent);
            } finally {
                if (alias != null) {
                    AliasVariableMapper.removeAliasesExposedToRequest(context, alias.getId());
                }
            }
        } else {
            super.broadcast(event);
        }
    }

    @Override
    public void queueEvent(FacesEvent event) {
        event = new AliasEvent(this, event);
        super.queueEvent(event);
    }

    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId, ContextCallback callback)
            throws FacesException {
        AliasVariableMapper alias = getAliasVariableMapper(context);
        try {
            AliasVariableMapper.exposeAliasesToRequest(context, alias);
            return super.invokeOnComponent(context, clientId, callback);
        } finally {
            if (alias != null) {
                AliasVariableMapper.removeAliasesExposedToRequest(context, alias.getId());
            }
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        AliasVariableMapper alias = getAliasVariableMapper(context);
        AliasVariableMapper.exposeAliasesToRequest(context, alias);
        super.encodeBegin(context);
    }

    @Override
    public void encodeChildren(final FacesContext context) throws IOException {
        // no need to expose variables: already done in #encodeBegin
        processFacetsAndChildren(context, PhaseId.RENDER_RESPONSE);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        super.encodeEnd(context);
        AliasVariableMapper alias = getAliasVariableMapper(context);
        if (alias != null) {
            AliasVariableMapper.removeAliasesExposedToRequest(context, alias.getId());
        }
    }

    @Override
    public void processDecodes(FacesContext context) {
        if (context == null) {
            throw new NullPointerException();
        }

        // Skip processing if our rendered flag is false
        if (!isRendered()) {
            return;
        }

        // XXX: decode component itself first, so that potential submitted
        // value is accurately exposed in context for facets and children
        try {
            decode(context);
        } catch (RuntimeException e) {
            context.renderResponse();
            throw e;
        }

        processFacetsAndChildrenWithVariable(context, PhaseId.APPLY_REQUEST_VALUES);

        if (isImmediate()) {
            executeValidate(context);
        }
    }

    @Override
    public void processValidators(FacesContext context) {
        if (context == null) {
            throw new NullPointerException();
        }

        // Skip processing if our rendered flag is false
        if (!isRendered()) {
            return;
        }

        processFacetsAndChildrenWithVariable(context, PhaseId.PROCESS_VALIDATIONS);

        if (!isImmediate()) {
            executeValidate(context);
        }
    }

    /**
     * Executes validation logic.
     */
    private void executeValidate(FacesContext context) {
        try {
            validate(context);
        } catch (RuntimeException e) {
            context.renderResponse();
            throw e;
        }

        if (!isValid()) {
            context.renderResponse();
        }
    }

    @Override
    public void processUpdates(FacesContext context) {
        if (context == null) {
            throw new NullPointerException();
        }

        // Skip processing if our rendered flag is false
        if (!isRendered()) {
            return;
        }

        processFacetsAndChildrenWithVariable(context, PhaseId.UPDATE_MODEL_VALUES);

        if (Boolean.TRUE.equals(getSubmitValue())) {
            try {
                updateModel(context);
            } catch (RuntimeException e) {
                context.renderResponse();
                throw e;
            }
        }

        if (!isValid()) {
            context.renderResponse();
        }
    }

    protected final void processFacetsAndChildren(final FacesContext context, final PhaseId phaseId) {
        List<UIComponent> stamps = getChildren();
        for (UIComponent stamp : stamps) {
            processComponent(context, stamp, phaseId);
        }
    }

    protected final void processFacetsAndChildrenWithVariable(final FacesContext context, final PhaseId phaseId) {
        AliasVariableMapper alias = getAliasVariableMapper(context);
        try {
            AliasVariableMapper.exposeAliasesToRequest(context, alias);
            processFacetsAndChildren(context, phaseId);
        } finally {
            if (alias != null) {
                AliasVariableMapper.removeAliasesExposedToRequest(context, alias.getId());
            }
        }
    }

    protected final void processComponent(FacesContext context, UIComponent component, PhaseId phaseId) {
        if (component != null) {
            if (phaseId == PhaseId.APPLY_REQUEST_VALUES) {
                component.processDecodes(context);
            } else if (phaseId == PhaseId.PROCESS_VALIDATIONS) {
                component.processValidators(context);
            } else if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
                component.processUpdates(context);
            } else if (phaseId == PhaseId.RENDER_RESPONSE) {
                try {
                    ComponentSupport.encodeRecursive(context, component);
                } catch (IOException err) {
                    log.error("Error while rendering component " + component);
                }
            } else {
                throw new IllegalArgumentException("Bad PhaseId:" + phaseId);
            }
        }
    }

    // properties management

    public String getVar() {
        if (var != null) {
            return var;
        }
        ValueExpression ve = getValueExpression("var");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setVar(String var) {
        this.var = var;
    }

    public Boolean getSubmitValue() {
        if (submitValue != null) {
            return submitValue;
        }
        ValueExpression ve = getValueExpression("submitValue");
        if (ve != null) {
            try {
                return Boolean.valueOf(Boolean.TRUE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return Boolean.TRUE;
        }
    }

    public void setSubmitValue(Boolean submitValue) {
        this.submitValue = submitValue;
    }

    public Object getValueToExpose() {
        Object value = getSubmittedValue();
        if (value == null) {
            // get original value bound
            value = super.getValue();
        }
        return value;
    }

    protected AliasVariableMapper getAliasVariableMapper(FacesContext ctx) {
        String var = getVar();
        Object value = getValueToExpose();
        AliasVariableMapper alias = new AliasVariableMapper();
        // reuse facelets id set on component
        String aliasId = getFaceletId();
        alias.setId(aliasId);
        alias.setVariable(var, ctx.getApplication().getExpressionFactory().createValueExpression(value, Object.class));
        return alias;
    }

    // state holder

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        var = (String) values[1];
        submitValue = (Boolean) values[2];
        submittedValue = values[3];
    }

    /**
     * Saves the locally set literal values kept on the component (from standard tags attributes) and since 5.6, also
     * saves the submitted value as {@link UIInput#saveState(FacesContext)} does not do it (see NXP-8898).
     */
    @Override
    public Object saveState(FacesContext context) {
        return new Object[] { super.saveState(context), var, submitValue, getSubmittedValue() };
    }

    /**
     * Resets the value holder local values
     *
     * @since 5.7
     */
    @Override
    public void resetCachedModel() {
        if (getValueExpression("value") != null) {
            setValue(null);
            setLocalValueSet(false);
        }
        setSubmittedValue(null);
    }

    @Override
    public boolean visitTree(VisitContext visitContext, VisitCallback callback) {
        if (!isVisitable(visitContext)) {
            return false;
        }
        FacesContext facesContext = visitContext.getFacesContext();
        AliasVariableMapper alias = getAliasVariableMapper(facesContext);
        try {
            AliasVariableMapper.exposeAliasesToRequest(facesContext, alias);
            return super.visitTree(visitContext, callback);
        } finally {
            if (alias != null) {
                AliasVariableMapper.removeAliasesExposedToRequest(facesContext, alias.getId());
            }
        }
    }

    public String getFaceletId() {
        return (String) getAttributes().get(ComponentSupport.MARK_CREATED);
    }

    public NuxeoValueHolderBean lookupBean(FacesContext ctx) {
        String expr = "#{" + NuxeoValueHolderBean.NAME + "}";
        NuxeoValueHolderBean bean = (NuxeoValueHolderBean) ctx.getApplication().evaluateExpressionGet(ctx, expr,
                Object.class);
        if (bean == null) {
            log.error("Managed bean not found: " + expr);
            return null;
        }
        return bean;
    }

    protected void saveToBean(Object value) {
        if (getFaceletId() == null) {
            // not added to the view yet, do not bother
            return;
        }
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            NuxeoValueHolderBean bean = lookupBean(ctx);
            if (bean != null) {
                bean.saveState(this, value);
            }
        }
    }

    @Override
    public void setSubmittedValue(Object submittedValue) {
        this.submittedValue = submittedValue;
        saveToBean(submittedValue);
    }

    @Override
    public Object getSubmittedValue() {
        return submittedValue;
    }

    @Override
    public void setValue(Object value) {
        super.setValue(value);
        saveToBean(value);
    }

}
