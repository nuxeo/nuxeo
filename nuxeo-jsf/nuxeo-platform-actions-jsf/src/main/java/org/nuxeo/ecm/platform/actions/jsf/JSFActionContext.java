/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.actions.jsf;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.AbstractActionContext;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;

/**
 * @since 5.7.3
 */
public class JSFActionContext extends AbstractActionContext implements ActionContext {

    private static final long serialVersionUID = 1L;

    protected final ELContext originalContext;

    protected final ExpressionFactory expressionFactory;

    public JSFActionContext(FacesContext faces) {
        super();
        this.originalContext = faces.getELContext();
        this.expressionFactory = faces.getApplication().getExpressionFactory();
    }

    public JSFActionContext(ELContext originalContext, ExpressionFactory expressionFactory) {
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
        ELContext finalContext = new JSFELContext(originalContext);
        VariableMapper vm = finalContext.getVariableMapper();
        // init default variables
        ValueExpression documentExpr = expressionFactory.createValueExpression(getCurrentDocument(),
                DocumentModel.class);
        ValueExpression userExpr = expressionFactory.createValueExpression(getCurrentPrincipal(), NuxeoPrincipal.class);
        vm.setVariable("actionContextDocument", documentExpr);
        // add variables originally exposed by the action framework,
        // do not add aliases currentDocument and currentUser here as they
        // should already be available in this JSF context
        vm.setVariable("document", documentExpr);
        vm.setVariable("principal", userExpr);
        // get custom context from ActionContext
        for (String key : localVariables.keySet()) {
            vm.setVariable(key, expressionFactory.createValueExpression(getLocalVariable(key), Object.class));
        }
        // expose Seam context for compatibility, although available its
        // components should be natively exposed in this JSF context
        putLocalVariable("SeamContext", new SeamContextHelper());

        // evaluate expression
        ValueExpression ve = expressionFactory.createValueExpression(finalContext, expr, Boolean.class);
        return Boolean.TRUE.equals(ve.getValue(finalContext));
    }

}
