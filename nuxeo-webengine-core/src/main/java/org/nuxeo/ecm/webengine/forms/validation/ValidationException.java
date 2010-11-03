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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.forms.validation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ValidationException extends Exception {

    private static final long serialVersionUID = 531665422854150881L;

    public static final String IS_REQUIRED_MSG = "is required";
    public static final String IS_INVALID_MSG = "is invalid";

    protected Map<String, String> invalidFields;
    protected Map<String, String> requiredFields;

    protected transient Form form; // the form that has errors - should be set by the caller

    public ValidationException(String message) {
        super(message);
        invalidFields = new HashMap<String, String>();
        requiredFields = new HashMap<String, String>();
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        invalidFields = new HashMap<String, String>();
        requiredFields = new HashMap<String, String>();
    }

    public ValidationException() {
        invalidFields = new HashMap<String, String>();
        requiredFields = new HashMap<String, String>();
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (message == null) {
            StringBuilder buf = new StringBuilder();
            if (hasRequiredFields()) {
                for (Map.Entry<String,String> entry : requiredFields.entrySet()) {
                    String msg = entry.getValue();
                    buf.append(entry.getKey()).append(": ").append(msg == null ? IS_REQUIRED_MSG : msg).append("\n");
                }
            }
            if (hasInvalidFields()) {
                for (Map.Entry<String,String> entry : invalidFields.entrySet()) {
                    String msg = entry.getValue();
                    buf.append(entry.getKey()).append(": ").append(msg == null ? IS_INVALID_MSG : msg).append("\n");
                }
            }
            return buf.toString();
        }
        return message;
    }

    public String getXmlMessage() {
        String message = super.getMessage();
        if (message == null) {
            StringBuilder buf = new StringBuilder();
            if (hasRequiredFields()) {
                for (Map.Entry<String,String> entry : requiredFields.entrySet()) {
                    String msg = entry.getValue();
                    buf.append("<li>").append(entry.getKey()).append(": ").append(msg == null ? IS_REQUIRED_MSG : msg);
                }
            }
            if (hasInvalidFields()) {
                for (Map.Entry<String,String> entry : invalidFields.entrySet()) {
                    String msg = entry.getValue();
                    buf.append("<li>").append(entry.getKey()).append(": ").append(msg == null ? IS_INVALID_MSG : msg);
                }
            }
            return buf.toString();
        }
        return message;
    }


    public ValidationException addRequiredField(String key) {
        requiredFields.put(key, null);
        return this;
    }

    public ValidationException addRequiredField(String key, String message) {
        requiredFields.put(key, message);
        return this;
    }

    public ValidationException addInvalidField(String key) {
        invalidFields.put(key, null);
        return this;
    }

    public ValidationException addInvalidField(String key, String message) {
        invalidFields.put(key, message);
        return this;
    }

    public boolean hasFieldErrors() {
        return !requiredFields.isEmpty() || !invalidFields.isEmpty();
    }

    public boolean hasInvalidFields() {
        return !invalidFields.isEmpty();
    }

    public boolean hasRequiredFields() {
        return !requiredFields.isEmpty();
    }

    public Collection<String> getRequiredFields() {
        return requiredFields.keySet();
    }

    public Collection<String> getInvalidFields() {
        return invalidFields.keySet();
    }

    public boolean hasErrors(String key) {
        return requiredFields.containsKey(key) || invalidFields.containsKey(key);
    }

    public String getError(String key) {
        String message = requiredFields.get(key);
        return message == null ? invalidFields.get(key) : message;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public Form getForm() {
        return form;
    }
}
