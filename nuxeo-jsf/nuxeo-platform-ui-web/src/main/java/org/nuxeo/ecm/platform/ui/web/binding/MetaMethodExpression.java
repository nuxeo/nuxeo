/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: MetaMethodExpression.java 28491 2008-01-04 19:04:30Z sfermigier $
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
import javax.el.MethodExpression;
import javax.el.MethodInfo;
import javax.el.MethodNotFoundException;
import javax.el.PropertyNotFoundException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

/**
 * Meta method expression used to invoke the EL expression that is already the result of a method expression.
 * <p>
 * For instance it is useful to use this expression to provide action links defined in NXActions extensions with links
 * like #{documentAction.createDocument('Domain')}.
 * <p>
 * There is no more than one level of abstraction:
 * <ul>
 * <li>the expression method value can be a standard method expression (with parameters or not);
 * <li>the expression method value can result in another expression method value after being invoke, in which case it is
 * reinvoked again using the same context;
 * <li>no further method invoking will be performed.
 * </ul>
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class MetaMethodExpression extends MethodExpression implements Serializable {

    private static final long serialVersionUID = -2721042412903607760L;

    private MethodExpression originalMethodExpression;

    private FunctionMapper fnMapper;

    private VariableMapper varMapper;

    private Class<?> returnType;

    private Class<?>[] paramTypes;

    public MetaMethodExpression(MethodExpression originalMethodExpression) {
        this(originalMethodExpression, null, null, null, null);
    }

    public MetaMethodExpression(MethodExpression originalMethodExpression, FunctionMapper fnMapper,
            VariableMapper varMapper, Class<?> returnType, Class<?>[] paramTypes) {
        this.originalMethodExpression = originalMethodExpression;
        this.fnMapper = fnMapper;
        this.varMapper = varMapper;
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    // Expression interface

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MetaMethodExpression)) {
            return false;
        }
        MetaMethodExpression other = (MetaMethodExpression) obj;
        return originalMethodExpression.equals(other.originalMethodExpression);
    }

    @Override
    public int hashCode() {
        return originalMethodExpression.hashCode();
    }

    @Override
    public String getExpressionString() {
        return originalMethodExpression.getExpressionString();
    }

    @Override
    public boolean isLiteralText() {
        return originalMethodExpression.isLiteralText();
    }

    // MethodExpression interface

    @Override
    public MethodInfo getMethodInfo(ELContext context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object invoke(ELContext context, Object[] params) {
        ELContext nxcontext = getLocalContext(context);
        FacesContext faces = FacesContext.getCurrentInstance();
        Application app = faces.getApplication();
        ExpressionFactory factory = app.getExpressionFactory();
        Object res = null;
        if (originalMethodExpression != null) {
            try {
                res = originalMethodExpression.invoke(nxcontext, params);
            } catch (MethodNotFoundException e) {
                // try to resolve as value expression first
                ValueExpression ve = factory.createValueExpression(nxcontext,
                        originalMethodExpression.getExpressionString(), String.class);
                res = ve.getValue(nxcontext);
            } catch (PropertyNotFoundException e) {
                // can be triggered from an AliasValueExpression, since it will not invoke a method expression at
                // resolution.
                throw e;
            }
            if (res instanceof String) {
                String expression = (String) res;
                if (ComponentTagUtils.isValueReference(expression)) {
                    MethodExpression newMeth = factory.createMethodExpression(nxcontext, expression, returnType,
                            paramTypes);
                    try {
                        res = newMeth.invoke(nxcontext, params);
                    } catch (ELException e) {
                        throw e;
                    } catch (RuntimeException e) {
                        throw new ELException(e);
                    }
                } else {
                    res = expression;
                }
            }
        }
        return res;
    }

    private ELContext getLocalContext(ELContext context) {
        if (fnMapper == null && varMapper == null) {
            return context;
        }
        return new org.nuxeo.ecm.platform.ui.web.binding.EvaluationContext(context, fnMapper, varMapper);
    }

    // Externalizable interface

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        originalMethodExpression = (MethodExpression) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(originalMethodExpression);
    }

}
