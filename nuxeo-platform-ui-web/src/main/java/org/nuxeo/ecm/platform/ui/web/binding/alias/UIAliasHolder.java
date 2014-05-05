/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.binding.alias;

import java.io.IOException;
import java.util.List;

import javax.faces.FacesException;
import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.faces.facelets.tag.jsf.ComponentSupport;

/**
 * Holder component for value expressions.
 * <p>
 * Exposes its expressions to the request context when rendered.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class UIAliasHolder extends UIOutput {

    private static final Log log = LogFactory.getLog(UIAliasHolder.class);

    public static final String COMPONENT_TYPE = UIAliasHolder.class.getName();

    public static final String COMPONENT_FAMILY = UIAliasHolder.class.getName();

    public UIAliasHolder() {
        super();
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    public void setRendererType(String rendererType) {
        // do nothing
    }

    public boolean isRendered() {
        return true;
    }

    public void setRendered(boolean rendered) {
        // do nothing
    }

    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void broadcast(FacesEvent event) {
        if (event instanceof AliasEvent) {
            FacesContext context = getFacesContext();
            AliasVariableMapper alias = getAliasVariableMapper();
            try {
                AliasVariableMapper.exposeAliasesToRequest(context, alias);
                FacesEvent origEvent = ((AliasEvent) event).getOriginalEvent();
                origEvent.getComponent().broadcast(origEvent);
            } finally {
                if (alias != null) {
                    AliasVariableMapper.removeAliasesExposedToRequest(context,
                            alias.getId());
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

    public boolean invokeOnComponent(FacesContext context, String clientId,
            ContextCallback callback) throws FacesException {
        AliasVariableMapper alias = getAliasVariableMapper();
        try {
            AliasVariableMapper.exposeAliasesToRequest(context, alias);
            return super.invokeOnComponent(context, clientId, callback);
        } finally {
            if (alias != null) {
                AliasVariableMapper.removeAliasesExposedToRequest(context,
                        alias.getId());
            }
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        AliasVariableMapper alias = getAliasVariableMapper();
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
        AliasVariableMapper alias = getAliasVariableMapper();
        if (alias != null) {
            AliasVariableMapper.removeAliasesExposedToRequest(context,
                    alias.getId());
        }
    }

    @Override
    public void processDecodes(FacesContext context) {
        processFacetsAndChildrenWithVariables(context,
                PhaseId.APPLY_REQUEST_VALUES);
    }

    @Override
    public void processValidators(FacesContext context) {
        processFacetsAndChildrenWithVariables(context,
                PhaseId.PROCESS_VALIDATIONS);
    }

    @Override
    public void processUpdates(FacesContext context) {
        processFacetsAndChildrenWithVariables(context,
                PhaseId.UPDATE_MODEL_VALUES);
    }

    protected final void processFacetsAndChildren(final FacesContext context,
            final PhaseId phaseId) {
        List<UIComponent> stamps = getChildren();
        for (UIComponent stamp : stamps) {
            processComponent(context, stamp, phaseId);
        }
    }

    protected final void processFacetsAndChildrenWithVariables(
            final FacesContext context, final PhaseId phaseId) {
        AliasVariableMapper alias = getAliasVariableMapper();
        try {
            AliasVariableMapper.exposeAliasesToRequest(context, alias);
            processFacetsAndChildren(context, phaseId);
        } finally {
            if (alias != null) {
                AliasVariableMapper.removeAliasesExposedToRequest(context,
                        alias.getId());
            }
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

    protected AliasVariableMapper getAliasVariableMapper() {
        Object value = getValue();
        if (value instanceof AliasVariableMapper) {
            return (AliasVariableMapper) value;
        } else if (value != null) {
            log.error("Invalid value: " + value);
        }
        return null;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        AliasVariableMapper alias = getAliasVariableMapper();
        try {
            AliasVariableMapper.exposeAliasesToRequest(context, alias);
            super.restoreState(context, state);
        } finally {
            if (alias != null) {
                AliasVariableMapper.removeAliasesExposedToRequest(context,
                        alias.getId());
            }
        }
    }

    @Override
    public Object saveState(FacesContext context) {
        AliasVariableMapper alias = getAliasVariableMapper();
        try {
            AliasVariableMapper.exposeAliasesToRequest(context, alias);
            return super.saveState(context);
        } finally {
            if (alias != null) {
                AliasVariableMapper.removeAliasesExposedToRequest(context,
                        alias.getId());
            }
        }
    }

    @Override
    public boolean visitTree(VisitContext visitContext, VisitCallback callback) {
        FacesContext facesContext = visitContext.getFacesContext();
        AliasVariableMapper alias = getAliasVariableMapper();
        try {
            AliasVariableMapper.exposeAliasesToRequest(facesContext, alias);
            return super.visitTree(visitContext, callback);
        } finally {
            if (alias != null) {
                AliasVariableMapper.removeAliasesExposedToRequest(facesContext,
                        alias.getId());
            }
        }
    }

}
