/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types.primitives;

import org.nuxeo.ecm.core.schema.types.PrimitiveType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class StringType extends PrimitiveType {

    public static final String ID = "string";

    public static final StringType INSTANCE = new StringType();

    private static final long serialVersionUID = -6451420665839530152L;

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
