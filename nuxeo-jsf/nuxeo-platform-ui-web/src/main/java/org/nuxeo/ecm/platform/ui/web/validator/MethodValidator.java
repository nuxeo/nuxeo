/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ui.web.validator;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.PartialStateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.MethodExpressionValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * Validator taking a method expression as attribute.
 * <p>
 * Equivalent to a f:validator tag, taking a validator id as attribute, expects it takes a bean validation method
 * binding as attribute.
 * <p>
 * Also handles conversion from a value expression to a method expression.
 *
 * @since 8.1
 */
public class MethodValidator implements Validator, PartialStateHolder {

    public static final String VALIDATOR_ID = "MethodValidator";

    private boolean initialState;

    /**
     * The value expression representing the validation method to call
     */
    protected MethodExpression method;

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (context == null) {
            throw new NullPointerException();
        }
        if (component == null) {
            throw new NullPointerException();
        }
        ValueExpression ve = component.getValueExpression("value");
        if (ve == null) {
            return;
        }

        MethodExpression method = getMethodExpression();
        if (method == null) {
            // no validation method resolved => ignore
            return;
        }

        new MethodExpressionValidator(method).validate(context, component, value);
    }

    public MethodExpression getMethodExpression() {
        return method;
    }

    public void setMethodExpression(MethodExpression method) {
        clearInitialState();
        this.method = method;
    }

    @Override
    public Object saveState(FacesContext context) {
        if (context == null) {
            throw new NullPointerException();
        }
        if (!initialStateMarked()) {
            Object values[] = new Object[1];
            values[0] = method;
            return (values);
        }
        return null;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        if (context == null) {
            throw new NullPointerException();
        }
        if (state != null) {
            Object values[] = (Object[]) state;
            method = (MethodExpression) values[0];
        }
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public void setTransient(boolean newTransientValue) {
        // NO-OP
    }

    @Override
    public void markInitialState() {
        initialState = true;
    }

    @Override
    public boolean initialStateMarked() {
        return initialState;
    }

    @Override
    public void clearInitialState() {
        initialState = false;
    }

}
