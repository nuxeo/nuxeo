/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation;

/**
 * @author Anahide Tchertchian
 */
public class TypeAdaptException extends OperationException {

    private static final long serialVersionUID = 1L;

    public TypeAdaptException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeAdaptException(String message) {
        super(message);
    }

    public TypeAdaptException(Throwable cause) {
        super(cause);
    }

}
