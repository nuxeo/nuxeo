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

package org.nuxeo.ecm.core.schema.types;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TypeBindingException extends TypeException {

    private static final long serialVersionUID = 3412654918664885706L;

    private final String typeName;


    public TypeBindingException(String typeName) {
        super("Type could not be resolved: " + typeName);
        this.typeName = typeName;
    }

    public TypeBindingException(String typeName, String message) {
        super("Type binding exception for " + typeName + ": " + message);
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

}
