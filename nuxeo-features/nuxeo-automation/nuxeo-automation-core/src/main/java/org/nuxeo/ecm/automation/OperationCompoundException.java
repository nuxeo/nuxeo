/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation;

/**
 * @since 5.7.2 Operation composite exception containing multiple @{link
 *        OperationException}
 */
public class OperationCompoundException extends OperationException {

    private static final long serialVersionUID = 1L;

    public final OperationException[] operationExceptions;

    public OperationCompoundException(String message,
            OperationException[] operationExceptions) {
        super(message);
        this.operationExceptions = operationExceptions;
    }
}
