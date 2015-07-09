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
package org.nuxeo.ecm.core.model;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Deprecated and never thrown, kept for compatibility so that old code catching this still works.
 * <p>
 * Use {@link org.nuxeo.ecm.core.api.DocumentNotFoundException} instead.
 *
 * @deprecated since 7.4, use org.nuxeo.ecm.core.api.DocumentNotFoundException instead
 */
@Deprecated
public class NoSuchDocumentException extends NuxeoException {

    private static final long serialVersionUID = 1L;

    public NoSuchDocumentException() {
        super();
    }

    public NoSuchDocumentException(String message) {
        super(message);
    }

    public NoSuchDocumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchDocumentException(Throwable cause) {
        super(cause);
    }

}
