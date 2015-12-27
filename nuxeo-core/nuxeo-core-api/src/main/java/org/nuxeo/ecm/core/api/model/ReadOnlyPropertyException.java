/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Exception thrown when attempting to write to a read-only property.
 */
public class ReadOnlyPropertyException extends PropertyException {

    private static final long serialVersionUID = 1L;

    public ReadOnlyPropertyException() {
        super();
    }

    public ReadOnlyPropertyException(String message) {
        super(message);
    }

    public ReadOnlyPropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadOnlyPropertyException(Throwable cause) {
        super(cause);
    }

}
