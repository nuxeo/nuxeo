/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.actions;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Default EL action context
 *
 * @since 5.7.3
 */
public class ELActionContext extends AbstractActionContext implements
        ActionContext {

    private static final long serialVersionUID = 1L;

    protected final ELContext originalContext;

    protected final ExpressionFactory expressionFactory;

    public ELActionContext(ELContext originalContext,
            ExpressionFactory expressionFactory) {
        super();
        this.originalContext = originalContext;
        this.expressionFactory = expressionFactory;
    }

    @Override
    public boolean checkCondition(String expression) throws ELException {
        if (StringUtils.isBlank(expression)) {
            return false;
        }
        expression = expression.trim();
        // compatibility code, as JEXL could resolve that kind of expression:
        // detect if expression is in brackets #{}, otherwise add it
        if (!expression.startsWith("#{") && !expression.startsWith("${")
        // don't confuse error messages in case of simple mistakes in the
        // expression
                && !expression.endsWith("}")) {
            expression = "#{" + expression + "}";
        }
        VariableMapper vm = originalContext.getVariableMapper();
        // init default variables
        ValueExpression documentExpr = expressionFactory.createValueExpression(
                getCurrentDocument(), DocumentModel.class);
        ValueExpression userExpr = expressionFactory.createValueExpression(
                getCurrentPrincipal(), NuxeoPrincipal.class);
        // add variables originally exposed by the action framework,
        // do not add aliases currentDocument and currentUser here as they
        // should already be available in this JSF context
        vm.setVariable("document", documentExpr);
        vm.setVariable("principal", userExpr);
        vm.setVariable("currentDocument", documentExpr);
        vm.setVariable("currentUser", userExpr);
        // get custom context from ActionContext
        for (String key : localVariables.keySet()) {
            vm.setVariable(key, expressionFactory.createValueExpression(
                    getLocalVariable(key), Object.class));
        }

        // evaluate expression
        ValueExpression expr = expressionFactory.createValueExpression(
                originalContext, expression, Boolean.class);
        return Boolean.TRUE.equals(expr.getValue(originalContext));
    }

}
