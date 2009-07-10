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
 */
package org.nuxeo.ecm.webengine.ui.wizard;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WizardException extends Exception {

    private static final long serialVersionUID = 1159502310247252315L;

    protected Map<String,String> errors;
    
    public WizardException() {
        this ("Validation Error");
    }

    public WizardException(String message) {
        super (message);
        this.errors = new HashMap<String, String>();
    }

    public WizardException(String message, Throwable t) {
        super (message, t);
        this.errors = new HashMap<String, String>();
    }

    public WizardException(Map<String,String> errors) {
        this ("Validation Error", errors);
    }

    public WizardException(String message, Map<String,String> errors) {
        super (message);
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public Set<String> getErrorFields() {
        return errors.keySet();
    }
    
    public String getError(String field) {
        return errors.get(field);
    }

    public void addError(String field, String error) {
        errors.put(field, error);
    }
    
    public boolean hasValidationErrors() {
        return errors.isEmpty();
    }
        
}
