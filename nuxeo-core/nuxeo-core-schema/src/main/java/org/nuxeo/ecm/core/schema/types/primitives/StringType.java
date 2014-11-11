/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.schema.types.primitives;

import org.nuxeo.ecm.core.schema.types.PrimitiveType;

/**
 * The string type.
 */
public final class StringType extends PrimitiveType {

    private static final long serialVersionUID = 1L;

    public static final String ID = "string";

    public static final StringType INSTANCE = new StringType();

    private StringType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        return true;
    }

    @Override
    public Object convert(Object value) {
        return value.toString();
    }

    @Override
    public Object decode(String str) {
        return str;
    }

    @Override
    public String encode(Object object) {
        return object != null ? object.toString() : "";
    }

    @Override
    public Object newInstance() {
        return "";
    }

    protected Object readResolve() {
        return INSTANCE;
    }

}
