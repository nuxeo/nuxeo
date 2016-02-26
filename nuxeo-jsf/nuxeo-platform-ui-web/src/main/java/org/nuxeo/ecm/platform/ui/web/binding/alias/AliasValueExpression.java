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

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Value expression that only stores a reference the {@link AliasVariableMapper} id so that the corresponding expression
 * is found in the context, and is evaluated against contextual values.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class AliasValueExpression extends ValueExpression {

    private static final Log log = LogFactory.getLog(AliasValueExpression.class);

    private static final long serialVersionUID = 1L;

    protected final String id;

    protected final String var;

    public AliasValueExpression(String id, String var) {
        this.id = id;
        this.var = var;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AliasValueExpression)) {
            return false;
        }
        AliasValueExpression other = (AliasValueExpression) obj;
        return id.equals(other.id) && var.equals(other.var);
    }

    @Override
    public int hashCode() {
        return id.hashCode() + var.hashCode();
    }

    @Override
    public String getExpressionString() {
        return null;
    }

    @Override
    public boolean isLiteralText() {
        return false;
    }

    @Override
    public Class<?> getExpectedType() {
        return Object.class;
    }

    @Override
    public Class<?> getType(ELContext context) {
        ValueExpression ve = resolveExpression(context);
        if (ve != null) {
            return ve.getType(context);
        }
        return Object.class;
    }

    /**
     * Looks up the {@link AliasVariableMapper} in the context, and if found, resolve the corresponding
     * {@link ValueExpression}.
     */
    @Override
    public Object getValue(ELContext context) {
        ValueExpression ve = resolveExpression(context);
        Object res = null;
        if (ve != null) {
            res = ve.getValue(context);
        }
        if (log.isDebugEnabled()) {
            log.debug("Resolved expression var='" + var + "' for mapper with id '" + id + "': " + res);
        }
        return res;
    }

    @Override
    public boolean isReadOnly(ELContext context) {
        ValueExpression ve = resolveExpression(context);
        if (ve != null) {
            return ve.isReadOnly(context);
        }
        return true;
    }

    @Override
    public void setValue(ELContext context, Object value) {
        ValueExpression ve = resolveExpression(context);
        if (ve != null) {
            ve.setValue(context, value);
        }
        if (log.isDebugEnabled()) {
            log.debug("Resolved expression var='" + var + "' for mapper with id '" + id + "' and set value: " + value);
        }
    }

    protected ValueExpression resolveExpression(ELContext context) {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        AliasVariableMapper vm = AliasVariableMapper.getVariableMapper(facesContext, id);

        if (vm == null) {
            if (log.isDebugEnabled()) {
                log.debug("No alias variable mapper with id '" + id + "' found in request for var '" + var + "'");
            }
            return null;
        }

        ValueExpression ve = vm.resolveVariable(var);
        if (ve == null) {
            log.error("Variable for var '" + var + "' not found in alias variable mapper with id '" + id + "'");
            return null;
        }

        return ve;
    }
}
