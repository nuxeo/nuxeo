/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model;

import org.nuxeo.ecm.core.api.PropertyException;

/**
 * Exception thrown when setting an illegal property value.
 */
public class InvalidPropertyValueException extends PropertyException {

    private static final long serialVersionUID = 1L;

    public InvalidPropertyValueException() {
        super();
    }

    public InvalidPropertyValueException(String message) {
        super(message);
    }

    public InvalidPropertyValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPropertyValueException(Throwable cause) {
        super(cause);
    }

}
