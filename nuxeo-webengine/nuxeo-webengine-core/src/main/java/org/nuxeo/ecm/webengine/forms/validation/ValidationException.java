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

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ValidationException extends Exception {

    private static final long serialVersionUID = 531665422854150881L;

    protected Collection<ErrorStatus> errors;

    public ValidationException(Collection<ErrorStatus> errors) {
        this.errors = errors;
    }

    @SuppressWarnings("unchecked")
    public ValidationException(ErrorStatus error) {
        this.errors = Collections.singleton(error);
    }

    /**
     * @return the errors.
     */
    public Collection<ErrorStatus> getErrors() {
        return errors;
    }


}
