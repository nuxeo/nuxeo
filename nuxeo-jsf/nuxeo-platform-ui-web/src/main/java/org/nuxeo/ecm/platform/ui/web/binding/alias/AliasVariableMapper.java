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

import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.facelets.el.VariableMapperWrapper;

/**
 * Variable mapper that holds value expressions. It can be exposed to the
 * request context after the JSF component tree build, so that
 * {@link AliasValueExpression} instances can be resolved.
 * <p>
 * It also builds the {@link VariableMapper} to use when building the component
 * tree, so that {@link AliasValueExpression} instances are kept in component
 * attributes, even on ajax rerender.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class AliasVariableMapper extends VariableMapper {

    private static final Log log = LogFactory.getLog(AliasVariableMapper.class);

    public static final String REQUEST_MARKER = AliasVariableMapper.class.getName()
            + "_MARKER";

    protected String id;

    protected Map<String, ValueExpression> vars;

    public AliasVariableMapper() {
        super();
    }

    public AliasVariableMapper(String id) {
        super();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ValueExpression resolveVariable(String variable) {
        ValueExpression ve = null;
        if (this.vars != null) {
            ve = this.vars.get(variable);
        }
        return ve;
    }

    public ValueExpression setVariable(String variable,
            ValueExpression expression) {
        if (this.vars == null) {
            this.vars = new HashMap<String, ValueExpression>();
        }
        return this.vars.put(variable, expression);
    }

    public VariableMapper getVariableMapperForBuild(VariableMapper orig) {
        VariableMapper vm = new VariableMapperWrapper(orig);
        if (this.vars != null) {
            for (Map.Entry<String, ValueExpression> var : this.vars.entrySet()) {
                vm.setVariable(var.getKey(), new AliasValueExpression(this.id,
                        var.getKey()));
            }
        }
        return vm;
    }

    public Map<String, ValueExpression> getVariables() {
        return vars;
    }

    @SuppressWarnings("unchecked")
    public static AliasVariableMapper getVariableMapper(
            FacesContext facesContext, String id) {
        ExternalContext ec = facesContext.getExternalContext();
        Map<String, AliasVariableMapper> mappers = (Map) ec.getRequestMap().get(
                AliasVariableMapper.REQUEST_MARKER);
        if (mappers == null) {
            return null;
        }
        return mappers.get(id);
    }

    @SuppressWarnings("unchecked")
    public static void exposeAliasesToRequest(FacesContext facesContext,
            AliasVariableMapper vm) {
        if (vm == null) {
            return;
        }
        String id = vm.getId();
        ExternalContext ec = facesContext.getExternalContext();
        Map<String, AliasVariableMapper> mappers = (Map) ec.getRequestMap().get(
                AliasVariableMapper.REQUEST_MARKER);
        if (mappers == null) {
            mappers = new HashMap<String, AliasVariableMapper>();
        }
        if (mappers.containsKey(id)) {
            if (log.isTraceEnabled()) {
                log.trace(String.format(
                        "Overriding alias variable mapper with id '%s'", id));
            }
        }
        mappers.put(id, vm);
        ec.getRequestMap().put(AliasVariableMapper.REQUEST_MARKER, mappers);
        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "Expose alias variable mapper with id '%s' to request: %s",
                    id, vm.getVariables()));
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeAliasesExposedToRequest(FacesContext facesContext,
            String id) {
        if (log.isTraceEnabled()) {
            log.trace(String.format(
                    "Remove alias variable mapper with id '%s' from request",
                    id));
        }
        if (id == null) {
            return;
        }
        ExternalContext ec = facesContext.getExternalContext();
        Map<String, AliasVariableMapper> mappers = (Map) ec.getRequestMap().get(
                AliasVariableMapper.REQUEST_MARKER);
        if (mappers != null) {
            mappers.remove(id);
        }
    }

}
