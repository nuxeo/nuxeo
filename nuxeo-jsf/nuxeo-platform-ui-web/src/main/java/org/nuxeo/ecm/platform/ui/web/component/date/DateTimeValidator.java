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
        this.transientValue = newTransientValue;
    }

    @Override
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {
        if ((context == null) || (component == null)) {
            throw new NullPointerException();
        }
        if (value != null) {
            Date date = (Date) value;
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            final int year = c.get(Calendar.YEAR);
            if (year > 9999) {
                throw new ValidatorException(MessageFactory.getMessage(context,
                        OUT_OF_RANGE_MESSAGE_ID, year));
            }
        }
    }
}
