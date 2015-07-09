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

/**
 * Exception thrown when trying to convert a property value to an incompatible type during read or write.
 */
public class PropertyConversionException extends InvalidPropertyValueException {

    private static final long serialVersionUID = 1L;

    public PropertyConversionException() {
        super();
    }

    public PropertyConversionException(String message) {
        super(message);
    }

    public PropertyConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyConversionException(Throwable cause) {
        super(cause);
    }

    public PropertyConversionException(Class<?> fromClass, Class<?> toClass) {
        this("Property Conversion failed from " + fromClass + " to " + toClass);
    }

    public PropertyConversionException(Class<?> fromClass, Class<?> toClass, String message) {
        this("Property Conversion failed from " + fromClass + " to " + toClass + ": " + message);
    }

}
