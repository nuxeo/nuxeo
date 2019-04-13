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
 * $Id: MethodValueExpression.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.FunctionMapper;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

/**
 * Method value expression encapsulates a method expression so that it invokes it when evaluated as a standard value
 * expression.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated method resolution is now supported by jboss-el
 */
@Deprecated
public class MethodValueExpression extends ValueExpression implements Externalizable {

    private static final long serialVersionUID = 1228707110702282837L;

    private FunctionMapper functionMapper;

    private VariableMapper variableMapper;

    private MethodExpression methodExpression;

    private Class<?>[] paramTypesClasses;

    public MethodValueExpression() {
    }

    public MethodValueExpression(FunctionMapper functionMapper, VariableMapper variableMapper,
            MethodExpression methodExpression, Class<?>[] paramTypesClasses) {
        this.functionMapper = functionMapper;
        this.variableMapper = variableMapper;
        this.methodExpression = methodExpression;
        this.paramTypesClasses = paramTypesClasses;
    }

    // Expression interface

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MethodValueExpression)) {
            return false;
        }
        MethodValueExpression other = (MethodValueExpression) obj;
        return methodExpression.equals(other.methodExpression);
    }

    @Override
    public int hashCode() {
        return methodExpression.hashCode();
    }

    @Override
    public String getExpressionString() {
        return methodExpression.getExpressionString();
    }

    @Override
    public boolean isLiteralText() {
        return methodExpression.isLiteralText();
    }

    // ValueExpression interface

    @Override
    public Class<?> getExpectedType() {
        return Object.class;
    }

    @Override
    public Class<?> getType(ELContext arg0) {
        return MethodExpression.class;
    }

    @Override
    public Object getValue(ELContext arg0) {
        // invoke method instead of resolving value
        try {
            EvaluationContext evalCtx = new EvaluationContext(arg0, functionMapper, variableMapper);
            return methodExpression.invoke(evalCtx, paramTypesClasses);
        } catch (RuntimeException e) {
            throw new ELException("Error while evaluation MethodValueExpression "
                    + methodExpression.getExpressionString(), e);
        }
    }

    @Override
    public boolean isReadOnly(ELContext arg0) {
        return true;
    }

    @Override
    public void setValue(ELContext arg0, Object arg1) {
        // do nothing
    }

    // Externalizable interface

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        methodExpression = (MethodExpression) in.readObject();
        paramTypesClasses = (Class[]) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(methodExpression);
        out.writeObject(paramTypesClasses);
    }

}
