/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.core.api;

/**
 * Deprecated and unused in Nuxeo, see NXP-17446.
 *
 * @deprecated since 7.4
 */
@Deprecated
public class DocumentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DocumentException() {
    }

    public DocumentException(String message) {
        super(message);
    }

    public DocumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentException(Throwable cause) {
        super(cause);
    }

}
