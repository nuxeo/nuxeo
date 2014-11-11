/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Exception indicating a property not found.
 */
public class PropertyNotFoundException extends PropertyException {

    private static final long serialVersionUID = 1L;

    protected final String detail;

    public PropertyNotFoundException(String path) {
        this(path, null);
    }

    public PropertyNotFoundException(String path, String detail) {
        super(path);
        this.detail = detail;
    }

    public String getPath() {
        return super.getMessage();
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String getMessage() {
        StringBuilder buf = new StringBuilder("Property not found: ");
        buf.append(getPath());
        if (detail != null) {
            buf.append(". ");
            buf.append(detail);
        }
        return buf.toString();
    }

}
