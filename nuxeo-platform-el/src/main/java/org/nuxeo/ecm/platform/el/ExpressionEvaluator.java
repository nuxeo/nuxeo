package org.nuxeo.ecm.platform.el;

import javax.el.ELContext;
import javax.el.ExpressionFactory;

public class ExpressionEvaluator {

    protected ExpressionEvaluator() {
        super();
    }

    public ExpressionEvaluator(ExpressionFactory factory) {
        this.expressionFactory = factory;
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
            throw new IllegalArgumentException("No value provided, cannot bind " + name + " in context " + context);
        }
        context.getVariableMapper().setVariable(
                name,
                expressionFactory.createValueExpression(value, value.getClass()));

    }
}
