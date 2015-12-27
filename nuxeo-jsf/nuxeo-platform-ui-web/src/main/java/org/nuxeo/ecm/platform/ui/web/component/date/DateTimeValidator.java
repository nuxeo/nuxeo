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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.platform.ui.web.component.date;

import java.util.Calendar;
import java.util.Date;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import com.sun.faces.util.MessageFactory;

/**
 * A date validator to invalidate dates of which year has five digits.
 *
 * @since 5.7.2
 */
public class DateTimeValidator implements Validator, StateHolder {

    public static final String VALIDATOR_ID = "NxDateValidator";

    public static final String OUT_OF_RANGE_MESSAGE_ID = "error.dateYear.invalidValue";

    private boolean transientValue = false;

    @Override
    public Object saveState(FacesContext context) {
        return null;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
    }

    @Override
    public boolean isTransient() {
        return transientValue;
    }

    @Override
    public void setTransient(boolean newTransientValue) {
        transientValue = newTransientValue;
    }

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if ((context == null) || (component == null)) {
            throw new NullPointerException();
        }
        if (value != null) {
            Date date = (Date) value;
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            final int year = c.get(Calendar.YEAR);
            if (year > 9999) {
                throw new ValidatorException(MessageFactory.getMessage(context, OUT_OF_RANGE_MESSAGE_ID, year));
            }
        }
    }
}
