/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public <T> T evaluateExpression(ELContext context, String stringExpression,
            Class<T> clazz) {
        return clazz.cast(expressionFactory.createValueExpression(context,
                stringExpression, clazz).getValue(context));
    }

    public void bindValue(ELContext context, String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "No value provided, cannot bind " + name + " in context " + context);
        }
        context.getVariableMapper().setVariable(
                name,
                expressionFactory.createValueExpression(value, value.getClass()));
    }

}
