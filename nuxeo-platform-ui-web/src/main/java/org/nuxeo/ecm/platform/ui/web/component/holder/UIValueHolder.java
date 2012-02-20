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
import javax.faces.context.FacesContext;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.binding.alias.AliasEvent;
import org.nuxeo.ecm.platform.ui.web.component.VariableManager;

import com.sun.facelets.tag.jsf.ComponentSupport;

/**
 * Component that keeps and exposes a value to the context during each JSF
 * phase.
 * <p>
 * Can be bound to a value as an input component, or not.
 *
 * @since 5.5
 */
public class UIValueHolder extends UIInput {

    private static final Log log = LogFactory.getLog(UIValueHolder.class);

    public static final String COMPONENT_TYPE = UIValueHolder.class.getName();

    public static final String COMPONENT_FAMILY = UIInput.COMPONENT_FAMILY;

    protected String var;

    protected Object defaultValue;

    protected Boolean submitValue;

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public String getRendererType() {
        return COMPONENT_TYPE;
    }

    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void broadcast(FacesEvent event) {
        if (event instanceof AliasEvent) {
            String var = getVar();
            Object origVarValue = VariableManager.saveRequestMapVarValue(var);
            try {
                VariableManager.putVariableToRequestParam(var,
                        getValueToExpose());
                FacesEvent origEvent = ((AliasEvent) event).getOriginalEvent();
                origEvent.getComponent().broadcast(origEvent);
            } finally {
                VariableManager.restoreRequestMapVarValue(var, origVarValue);
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

    public boolean invokeOnComponent(FacesContext context, String clientId,
            ContextCallback callback) throws FacesException {
        String var = getVar();
        Object origVarValue = VariableManager.saveRequestMapVarValue(var);
        try {
            VariableManager.putVariableToRequestParam(var, getValueToExpose());
            return super.invokeOnComponent(context, clientId, callback);
        } finally {
            VariableManager.restoreRequestMapVarValue(var, origVarValue);
        }
    }

    @Override
    public void encodeChildren(final FacesContext context) throws IOException {
        String var = getVar();
        Object origVarValue = VariableManager.saveRequestMapVarValue(var);
        try {
            VariableManager.putVariableToRequestParam(var, getValueToExpose());
            processFacetsAndChildren(context, PhaseId.RENDER_RESPONSE);
        } finally {
            VariableManager.restoreRequestMapVarValue(var, origVarValue);
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

        processFacetsAndChildrenWithVariable(context,
                PhaseId.APPLY_REQUEST_VALUES);

        // Process this component itself
        try {
            decode(context);
        } catch (RuntimeException e) {
            context.renderResponse();
            throw e;
        }

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

        processFacetsAndChildrenWithVariable(context,
                PhaseId.PROCESS_VALIDATIONS);

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

        processFacetsAndChildrenWithVariable(context,
                PhaseId.UPDATE_MODEL_VALUES);

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

    protected final void processFacetsAndChildren(final FacesContext context,
            final PhaseId phaseId) {
        List<UIComponent> stamps = getChildren();
        for (UIComponent stamp : stamps) {
            processComponent(context, stamp, phaseId);
        }
    }

    protected final void processFacetsAndChildrenWithVariable(
            final FacesContext context, final PhaseId phaseId) {
        String var = getVar();
        Object origVarValue = VariableManager.saveRequestMapVarValue(var);
        try {
            VariableManager.putVariableToRequestParam(var, getValueToExpose());
            processFacetsAndChildren(context, phaseId);
        } finally {
            VariableManager.restoreRequestMapVarValue(var, origVarValue);
        }
    }

    protected final void processComponent(FacesContext context,
            UIComponent component, PhaseId phaseId) {
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

    public Object getDefaultValue() {
        if (defaultValue != null) {
            return defaultValue;
        }
        ValueExpression ve = getValueExpression("defaultValue");
        if (ve != null) {
            try {
                return ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
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

    protected static Object getCurrentValue(UIInput comp) {
        Object submitted = comp.getSubmittedValue();
        if (submitted != null) {
            return submitted;
        }
        return comp.getValue();
    }

    public Object getValueToExpose() {
        Object value = super.getSubmittedValue();
        if (value == null) {
            // get original value bound
            value = super.getValue();
        }
        if (value == null) {
            // check defaultValue attribute
            Object defaultValue = getDefaultValue();
            if (defaultValue != null) {
                value = getDefaultValue();
            }
        }
        return value;
    }

    // state holder

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        var = (String) values[1];
        defaultValue = values[2];
        submitValue = (Boolean) values[3];
        setSubmittedValue(values[4]);
    }

    /**
     * Saves the locally set literal values kept on the component (from
     * standard tags attributes) and since 5.6, also saves the submitted value
     * as {@link UIInput#saveState(FacesContext)} does not do it (see
     * NXP-8898).
     */
    @Override
    public Object saveState(FacesContext context) {
        return new Object[] { super.saveState(context), var, defaultValue,
                submitValue, getSubmittedValue() };
    }

}
