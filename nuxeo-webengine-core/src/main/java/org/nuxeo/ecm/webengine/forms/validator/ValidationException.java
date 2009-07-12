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

package org.nuxeo.ecm.webengine.forms.validator;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ValidationException extends Exception {

    private static final long serialVersionUID = 531665422854150881L;

    protected Collection<String> fields;
    protected Collection<String> requiredFields;

    public ValidationException(String message) {
        super(message);
        fields = new ArrayList<String>();
        requiredFields = new ArrayList<String>();
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        fields = new ArrayList<String>();
        requiredFields = new ArrayList<String>();
    }

    public ValidationException() {
        fields = new ArrayList<String>();
        requiredFields = new ArrayList<String>();
    }
    
    public ValidationException addRequiredField(String field) {
        requiredFields.add(field);
        return this;
    }

    public ValidationException addField(String field) {
        fields.add(field);
        return this;
    }

    public boolean hasRequiredFields() {
        return !requiredFields.isEmpty();
    }

    public boolean hasFields() {
        return !fields.isEmpty();
    }
    
    public boolean isFieldException() {
        return hasFields() || hasRequiredFields();
    }

    public Collection<String> getFields() {
        return fields;
    }

    public Collection<String> getRequiredFields() {
        return requiredFields;
    }
    
}
