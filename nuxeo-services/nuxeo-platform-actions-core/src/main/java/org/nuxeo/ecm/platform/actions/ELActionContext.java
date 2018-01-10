/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.actions;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import org.apache.commons.lang3.StringUtils;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.el.ExpressionContext;

/**
 * Default EL action context
 *
 * @since 5.7.3
 */
public class ELActionContext extends AbstractActionContext implements ActionContext {

    private static final long serialVersionUID = 1L;

    protected final ELContext originalContext;

    protected final ExpressionFactory expressionFactory;

    public static final ExpressionFactory EXPRESSION_FACTORY = new ExpressionFactoryImpl();

    public ELActionContext() {
        this(new ExpressionContext(), EXPRESSION_FACTORY);
    }

    public ELActionContext(ELContext originalContext, ExpressionFactory expressionFactory) {
        super();
        this.originalContext = originalContext;
        this.expressionFactory = expressionFactory;
    }

    @Override
    public boolean checkCondition(String expression) throws ELException {
        if (StringUtils.isBlank(expression)) {
            return false;
        }
        String expr = expression.trim();
        // compatibility code, as JEXL could resolve that kind of expression:
        // detect if expression is in brackets #{}, otherwise add it
        if (!expr.startsWith("#{") && !expr.startsWith("${")
        // don't confuse error messages in case of simple mistakes in the
        // expression
                && !expr.endsWith("}")) {
            expr = "#{" + expr + "}";
        }
        VariableMapper vm = originalContext.getVariableMapper();
        // init default variables
        ValueExpression documentExpr = expressionFactory.createValueExpression(getCurrentDocument(),
                DocumentModel.class);
        ValueExpression userExpr = expressionFactory.createValueExpression(getCurrentPrincipal(), NuxeoPrincipal.class);
        // add variables originally exposed by the action framework,
        // do not add aliases currentDocument and currentUser here as they
        // should already be available in this JSF context
        vm.setVariable("document", documentExpr);
        vm.setVariable("principal", userExpr);
        vm.setVariable("currentDocument", documentExpr);
        vm.setVariable("currentUser", userExpr);
        // get custom context from ActionContext
        for (String key : localVariables.keySet()) {
            vm.setVariable(key, expressionFactory.createValueExpression(getLocalVariable(key), Object.class));
        }

        // evaluate expression
        ValueExpression ve = expressionFactory.createValueExpression(originalContext, expr, Boolean.class);
        return Boolean.TRUE.equals(ve.getValue(originalContext));
    }

}
