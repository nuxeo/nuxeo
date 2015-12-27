/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.el;

import javax.el.ELContext;
import javax.el.ExpressionFactory;

public class ExpressionEvaluator {

    protected ExpressionEvaluator() {
    }

    public ExpressionEvaluator(ExpressionFactory factory) {
        expressionFactory = factory;
    }

    protected ExpressionFactory expressionFactory;

    public void setExpressionFactory(ExpressionFactory expressionFactory) {
        this.expressionFactory = expressionFactory;
    }

    public <T> T evaluateExpression(ELContext context, String stringExpression, Class<T> clazz) {
        return clazz.cast(expressionFactory.createValueExpression(context, stringExpression, clazz).getValue(context));
    }

    public void bindValue(ELContext context, String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("No value provided, cannot bind " + name + " in context " + context);
        }
        // the jsf/facelets way of binding additional values in the context
        // is to contribute a variable mapper wrapping the existing one, so
        // that contexts are not merged and variables are not overridden,
        // especially when the variable mapper is used in a shared context =>
        // maybe change this behaviour if needed
        context.getVariableMapper().setVariable(name, expressionFactory.createValueExpression(value, value.getClass()));
    }

}
