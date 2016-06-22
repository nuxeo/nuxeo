/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: MetaValueExpression.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * Meta value expression used to invoke the EL expression that is already the result of a value expression.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MetaValueExpression extends ValueExpression implements Serializable {

    private static final long serialVersionUID = -2721042412903607760L;

    private static final Log log = LogFactory.getLog(MetaValueExpression.class);

    private ValueExpression originalValueExpression;

    private FunctionMapper fnMapper;

    private VariableMapper varMapper;

    private Class<?> expectedType;

    /**
     * @see {@link #MetaValueExpression(ValueExpression, FunctionMapper, VariableMapper)}
     */
    public MetaValueExpression(ValueExpression originalValueExpression) {
        this(originalValueExpression, null, null, Object.class);
    }

    public MetaValueExpression(ValueExpression originalValueExpression, FunctionMapper fnMapper,
            VariableMapper varMapper) {
        this(originalValueExpression, fnMapper, varMapper, Object.class);
    }

    public MetaValueExpression(ValueExpression originalValueExpression, FunctionMapper fnMapper,
            VariableMapper varMapper, Class<?> expectedType) {
        this.originalValueExpression = originalValueExpression;
        this.fnMapper = fnMapper;
        this.varMapper = varMapper;
        this.expectedType = expectedType;
    }

    // Expression interface

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MetaValueExpression)) {
            return false;
        }
        MetaValueExpression other = (MetaValueExpression) obj;
        return originalValueExpression.equals(other.originalValueExpression);
    }

    @Override
    public int hashCode() {
        return originalValueExpression.hashCode();
    }

    @Override
    public String getExpressionString() {
        return originalValueExpression.getExpressionString();
    }

    @Override
    public boolean isLiteralText() {
        // XXX should invoke first
        return originalValueExpression.isLiteralText();
    }

    // ValueExpression interface

    @Override
    public Class<?> getExpectedType() {
        // XXX should invoke first
        return originalValueExpression.getExpectedType();
    }

    private ELContext getLocalContext(ELContext context) {
        if (fnMapper == null && varMapper == null) {
            return context;
        }
        return new org.nuxeo.ecm.platform.ui.web.binding.EvaluationContext(context, fnMapper, varMapper);
    }

    @Override
    public Class<?> getType(ELContext context) {
        ELContext nxcontext = getLocalContext(context);
        // XXX should invoke first...
        return originalValueExpression.getType(nxcontext);
    }

    @Override
    public Object getValue(ELContext context) {
        ELContext nxcontext = getLocalContext(context);
        Object res = null;
        if (originalValueExpression != null) {
            res = originalValueExpression.getValue(nxcontext);
            if (res instanceof String) {
                String expression = (String) res;
                if (ComponentTagUtils.isValueReference(expression)) {
                    FacesContext faces = FacesContext.getCurrentInstance();
                    Application app = faces.getApplication();
                    ExpressionFactory factory = app.getExpressionFactory();
                    ValueExpression newExpr = factory.createValueExpression(nxcontext, expression, expectedType);
                    try {
                        res = newExpr.getValue(nxcontext);
                    } catch (ELException err) {
                        log.error("Error processing expression " + expression + ": " + err);
                        res = null;
                    }
                } else {
                    res = expression;
                }
            }
        }
        return res;
    }

    @Override
    public boolean isReadOnly(ELContext context) {
        return true;
    }

    @Override
    public void setValue(ELContext context, Object value) {
        // do nothing
    }

    // Externalizable interface

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        originalValueExpression = (ValueExpression) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(originalValueExpression);
    }

}
